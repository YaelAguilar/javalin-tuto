package org.example.services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.example.dtos.auth.LoginRequest;
import org.example.dtos.auth.LoginResponse;
import org.example.exceptions.BadRequestException;
import org.example.exceptions.UnauthorizedException;
import org.example.mappers.UserMapper;
import org.example.repositories.UserRepository;
import org.example.utils.JWTUtil;

public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        if (loginRequest.email() == null || loginRequest.email().trim().isEmpty() ||
                loginRequest.password() == null || loginRequest.password().trim().isEmpty()) {
            throw new BadRequestException("Email and password are required.");
        }

        return userRepository.findByEmail(loginRequest.email().trim())
                .filter(user -> verifyPassword(loginRequest.password(), user.getPassword()))
                .map(user -> {
                    String token = JWTUtil.generateToken(user);
                    return new LoginResponse(token, userMapper.toUserDTO(user));
                })
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials."));
    }

    public void logout(String token) {
        if (JWTUtil.isTokenValid(token)) {
            JWTUtil.blacklistToken(token);
        }
    }

    private boolean verifyPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.verifyer().verify(rawPassword.toCharArray(), hashedPassword).verified;
        } catch (Exception e) {
            System.err.println("Error during password verification: " + e.getMessage());
            return false;
        }
    }
}