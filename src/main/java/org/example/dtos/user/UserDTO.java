package org.example.dtos.user;

import org.example.models.Role;

import java.time.LocalDateTime;

public record UserDTO(
        int id,
        String firstName,
        String middleName,
        String lastName,
        String email,
        Role role,
        LocalDateTime createdAt
) {
}