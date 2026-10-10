package com.finpath.backend.service;

import com.finpath.backend.dto.AuthDtos.*;
import com.finpath.backend.entity.User;
import com.finpath.backend.repository.UserRepository;
import com.finpath.backend.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(
            UserRepository repo,
            PasswordEncoder encoder,
            JwtService jwt
    ) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (repo.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists."
            );
        }

        User user = new User();

        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(
                encoder.encode(request.password())
        );

        repo.save(user);

        String token = jwt.generate(
                user.getId(),
                user.getEmail()
        );

        return new AuthResponse(
                token,
                toResponse(user)
        );
    }

    public AuthResponse login(LoginRequest request) {

        User user = repo
                .findByEmailIgnoreCase(
                        request.email().trim().toLowerCase()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or password."
                        )
                );

        if (!encoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "Invalid email or password."
            );
        }

        String token = jwt.generate(
                user.getId(),
                user.getEmail()
        );

        return new AuthResponse(
                token,
                toResponse(user)
        );
    }

    public UserResponse me(String id) {

        User user = repo.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found."
                        )
                );

        return toResponse(user);
    }

    public void forgot(ForgotPasswordRequest request) {
        // Password reset email can be implemented later.
    }

    private UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName()
        );
    }
}