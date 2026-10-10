package com.finpath.backend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> bad(
            IllegalArgumentException e) {

        e.printStackTrace();

        String message = e.getMessage();

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "error",
                        message == null
                                ? "Invalid request"
                                : message
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> any(
            Exception e) {

        // IMPORTANT:
        // Print the real backend error in the Spring Boot terminal.
        e.printStackTrace();

        String message = e.getMessage();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error",
                        message == null
                                ? e.getClass().getSimpleName()
                                : message
                ));
    }
}