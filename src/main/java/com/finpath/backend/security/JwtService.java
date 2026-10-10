package com.finpath.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expiryMs
    ) {

        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "app.jwt.secret must be at least 32 characters"
            );
        }

        this.key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expiryMs = expiryMs;
    }

    public String generate(String userId, String email) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(
                        new Date(
                                now.toEpochMilli() + expiryMs
                        )
                )
                .signWith(key)
                .compact();
    }

    public String extractUserId(String token) {

        return parse(token)
                .getPayload()
                .getSubject();
    }

    public boolean isValid(String token) {

        try {
            parse(token);
            return true;

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Jws<Claims> parse(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }
}