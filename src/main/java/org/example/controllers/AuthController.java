package org.example.controllers;

import io.javalin.http.Context;
import org.example.dtos.auth.LoginRequest;
import org.example.dtos.auth.LoginResponse;
import org.example.services.AuthService;
import org.example.utils.JWTUtil;
import java.util.Map;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void login(Context ctx) {
        LoginRequest loginRequest = ctx.bodyAsClass(LoginRequest.class);
        LoginResponse loginResponse = authService.login(loginRequest);
        ctx.status(200).json(Map.of(
                "success", true,
                "message", "Login successful",
                "data", loginResponse
        ));
    }

    public void logout(Context ctx) {
        String token = JWTUtil.extractTokenFromHeader(ctx.header("Authorization"));
        authService.logout(token);
        ctx.status(200).json(Map.of("success", true, "message", "Logout successful"));
    }
}