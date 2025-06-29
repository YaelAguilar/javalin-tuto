package org.example.dtos.auth;

public record RegisterRequest(
        String firstName,
        String middleName,
        String lastName,
        String email,
        String password,
        String confirmPassword
) {
}