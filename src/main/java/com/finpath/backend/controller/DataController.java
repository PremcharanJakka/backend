package com.finpath.backend.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/data")
public class DataController {

    /*
     * Tables that the frontend is allowed to access.
     */
    private static final Set<String> TABLES = Set.of(
            "profiles",
            "expenses",
            "debts",
            "emergency_fund",
            "insurance",
            "investments",
            "goals",
            "income_sources",
            "fire_plans",
            "budget_plans",
            "family_info"
    );

    /*
     * Columns that the API is allowed to read/write.
     */
    private static final Set<String> COLUMNS = Set.of(
            "id",
            "user_id",

            // profiles
            "full_name",
            "monthly_income",
            "currency",
            "risk_tolerance",

            // expenses
            "amount",
            "category",
            "description",
            "date",
            "type",

            // common
            "name",

            // debts
            "principal",
            "interest_rate",
            "tenure_months",
            "emi_amount",
            "remaining_balance",
            "start_date",

            // emergency fund
            "target_months",
            "current_corpus",
            "monthly_expenses",

            // insurance
            "coverage_amount",
            "premium_amount",
            "premium_frequency",
            "maturity_date",
            "notes",

            // investments
            "invested_amount",
            "current_value",
            "expected_return",

            // goals
            "target_amount",
            "current_amount",
            "target_date",
            "color",

            // income
            "frequency",

            // FIRE
            "current_age",
            "target_fire_age",
            "annual_income",
            "salary_growth_rate",
            "annual_expenses",
            "current_investments",
            "withdrawal_rate",
            "inflation_rate",

            // budget
            "salary",
            "additional_income",
            "housing",
            "loans_emis",
            "utilities",
            "food",
            "education",
            "insurance",
            "investments",
            "savings",
            "other",

            // family information / JSON fields
            "emergency_contacts",
            "insurance_policies",
            "bank_details",
            "investment_details",
            "property_info",
            "documents",
            "identity_details",
            "liabilities",
            "will_info",
            "power_of_attorney",
            "emergency_info",
            "digital_accounts",
            "important_dates",
            "funeral_wishes",
            "family_tree",

            // timestamps
            "created_at",
            "updated_at"
    );

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public DataController(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    // ============================================================
    // GET
    // ============================================================

    @GetMapping("/{table}")
    public List<Map<String, Object>> list(
            @PathVariable String table,
            @RequestParam Map<String, String> params,
            Authentication auth
    ) {

        checkTable(table);

        String userId = auth.getName();

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM " + table + " WHERE user_id=?"
        );

        List<Object> args = new ArrayList<>();
        args.add(userId);

        /*
         * Apply filters.
         *
         * Example:
         * /api/data/goals?id=123
         */
        for (var entry : params.entrySet()) {

            String column = entry.getKey();

            if (column.equals("order")
                    || column.equals("ascending")
                    || column.equals("single")) {
                continue;
            }

            checkColumn(column);

            sql.append(" AND ")
                    .append(column)
                    .append("=?");

            args.add(coerce(entry.getValue()));
        }

        /*
         * Ordering.
         */
        if (params.containsKey("order")) {

            String order = params.get("order");

            checkColumn(order);

            boolean ascending =
                    !"false".equalsIgnoreCase(
                            params.getOrDefault("ascending", "true")
                    );

            sql.append(" ORDER BY ")
                    .append(order)
                    .append(ascending ? " ASC" : " DESC");
        }

        return jdbc.queryForList(
                sql.toString(),
                args.toArray()
        );
    }

    // ============================================================
    // INSERT
    // ============================================================

    @PostMapping("/{table}")
    public List<Map<String, Object>> insert(
            @PathVariable String table,
            @RequestBody Map<String, Object> body,
            Authentication auth
    ) {

        checkTable(table);

        /*
         * Make a copy so that the original request body
         * is not modified unexpectedly.
         */
        body = new LinkedHashMap<>(body);

        /*
         * Generate ID if frontend did not send one.
         */
        body.putIfAbsent(
                "id",
                UUID.randomUUID().toString()
        );

        /*
         * Always use the authenticated user.
         *
         * This is important for security and fixing the
         * "user_id doesn't have a default value" error.
         */
        body.put(
                "user_id",
                auth.getName()
        );

        /*
         * Database automatically handles timestamps.
         */
        body.remove("created_at");
        body.remove("updated_at");

        Map<String, Object> clean = cleanBody(body);

        if (clean.isEmpty()) {
            throw new IllegalArgumentException(
                    "No fields supplied"
            );
        }

        List<String> columns =
                new ArrayList<>(clean.keySet());

        List<Object> values = new ArrayList<>();

        for (String column : columns) {
            values.add(
                    dbValue(clean.get(column))
            );
        }

        String placeholders =
                String.join(
                        ",",
                        Collections.nCopies(
                                columns.size(),
                                "?"
                        )
                );

        String sql =
                "INSERT INTO "
                        + table
                        + " ("
                        + String.join(",", columns)
                        + ") VALUES ("
                        + placeholders
                        + ")";

        jdbc.update(
                sql,
                values.toArray()
        );

        /*
         * Return the inserted row.
         */
        String id =
                clean.containsKey("id")
                        ? String.valueOf(clean.get("id"))
                        : null;

        if (id != null) {
            return byId(
                    table,
                    id,
                    auth
            );
        }

        return latest(
                table,
                auth
        );
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @PatchMapping("/{table}")
    public List<Map<String, Object>> update(
            @PathVariable String table,
            @RequestParam(required = false) String id,
            @RequestBody Map<String, Object> body,
            Authentication auth
    ) {

        checkTable(table);

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "id is required"
            );
        }

        body = new LinkedHashMap<>(body);

        /*
         * Never allow the frontend to change ownership.
         */
        body.remove("id");
        body.remove("user_id");

        /*
         * Timestamps are controlled by the database.
         */
        body.remove("created_at");

        Map<String, Object> clean =
                cleanBody(body);

        /*
         * Nothing to update.
         */
        if (clean.isEmpty()) {
            return byId(
                    table,
                    id,
                    auth
            );
        }

        List<String> sets =
                new ArrayList<>();

        List<Object> args =
                new ArrayList<>();

        for (var entry : clean.entrySet()) {

            sets.add(
                    entry.getKey() + "=?"
            );

            args.add(
                    dbValue(entry.getValue())
            );
        }

        /*
         * Parameters for:
         *
         * WHERE id=? AND user_id=?
         */
        args.add(id);
        args.add(auth.getName());

        String sql =
                "UPDATE "
                        + table
                        + " SET "
                        + String.join(",", sets)
                        + " WHERE id=? AND user_id=?";

        jdbc.update(
                sql,
                args.toArray()
        );

        /*
         * Return updated row.
         */
        return byId(
                table,
                id,
                auth
        );
    }

    // ============================================================
    // DELETE
    // ============================================================

    @DeleteMapping("/{table}")
    public Map<String, Object> delete(
            @PathVariable String table,
            @RequestParam String id,
            Authentication auth
    ) {

        checkTable(table);

        int affected =
                jdbc.update(
                        "DELETE FROM "
                                + table
                                + " WHERE id=? AND user_id=?",
                        id,
                        auth.getName()
                );

        return Map.of(
                "success",
                true,
                "deleted",
                affected > 0
        );
    }

    // ============================================================
    // GET ROW BY ID
    // ============================================================

    private List<Map<String, Object>> byId(
            String table,
            String id,
            Authentication auth
    ) {

        return jdbc.queryForList(
                "SELECT * FROM "
                        + table
                        + " WHERE id=? AND user_id=?",
                id,
                auth.getName()
        );
    }

    // ============================================================
    // GET LATEST ROW
    // ============================================================

    private List<Map<String, Object>> latest(
            String table,
            Authentication auth
    ) {

        return jdbc.queryForList(
                "SELECT * FROM "
                        + table
                        + " WHERE user_id=? "
                        + "ORDER BY created_at DESC LIMIT 1",
                auth.getName()
        );
    }

    // ============================================================
    // TABLE VALIDATION
    // ============================================================

    private void checkTable(String table) {

        if (!TABLES.contains(table)) {
            throw new IllegalArgumentException(
                    "Unsupported resource: " + table
            );
        }
    }

    // ============================================================
    // COLUMN VALIDATION
    // ============================================================

    private void checkColumn(String column) {

        if (!COLUMNS.contains(column)) {
            throw new IllegalArgumentException(
                    "Unsupported column: " + column
            );
        }
    }

    // ============================================================
    // CLEAN REQUEST BODY
    // ============================================================

    private Map<String, Object> cleanBody(
            Map<String, Object> body
    ) {

        Map<String, Object> output =
                new LinkedHashMap<>();

        for (var entry : body.entrySet()) {

            String column = entry.getKey();

            checkColumn(column);

            /*
             * IMPORTANT:
             *
             * user_id is intentionally NOT removed here.
             *
             * During INSERT we add the authenticated user_id
             * before calling this method.
             *
             * During UPDATE user_id is removed before this
             * method is called.
             */
            output.put(
                    column,
                    entry.getValue()
            );
        }

        return output;
    }

    // ============================================================
    // CONVERT JSON VALUES
    // ============================================================

    private Object dbValue(Object value) {

        if (value == null) {
            return null;
        }

        /*
         * Convert Java Map/List/Array into JSON string
         * for MySQL JSON/TEXT columns.
         */
        if (value instanceof Map
                || value instanceof List
                || value.getClass().isArray()) {

            try {

                return mapper.writeValueAsString(
                        value
                );

            } catch (JsonProcessingException e) {

                throw new IllegalArgumentException(
                        "Invalid JSON field"
                );
            }
        }

        return value;
    }

    // ============================================================
    // CONVERT QUERY PARAMETERS
    // ============================================================

    private Object coerce(String value) {

        if (value == null) {
            return null;
        }

        /*
         * Integer.
         */
        if (value.matches("^-?\\d+$")) {

            try {
                return Long.parseLong(value);
            } catch (Exception ignored) {
            }
        }

        /*
         * Decimal.
         */
        if (value.matches("^-?\\d+\\.\\d+$")) {

            try {
                return Double.parseDouble(value);
            } catch (Exception ignored) {
            }
        }

        return value;
    }
}