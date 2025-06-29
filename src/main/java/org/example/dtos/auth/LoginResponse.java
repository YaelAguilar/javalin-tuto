package org.example.dtos.auth;

import org.example.dtos.user.UserDTO;

public record LoginResponse(String token, UserDTO user) {
}