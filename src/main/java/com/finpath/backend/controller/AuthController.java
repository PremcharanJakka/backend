package com.finpath.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finpath.backend.dto.AuthDtos.ForgotPasswordRequest;
import com.finpath.backend.dto.AuthDtos.LoginRequest;
import com.finpath.backend.dto.AuthDtos.RegisterRequest;
import com.finpath.backend.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        try {
            return ResponseEntity.ok(
                    authService.register(request)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            e.getMessage()
                    ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request
    ) {
        try {
            return ResponseEntity.ok(
                    authService.login(request)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            e.getMessage()
                    ));
        }
    }

    @GetMapping("/me")
public ResponseEntity<?> me(
        org.springframework.security.core.Authentication authentication
) {
    try {
        String userId = authentication.getName();

        return ResponseEntity.ok(
                authService.me(userId)
        );

    } catch (IllegalArgumentException e) {
        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "message",
                        e.getMessage()
                ));
    }
}

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.forgot(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "If the account exists, password reset instructions will be sent."
                )
        );
    }
}