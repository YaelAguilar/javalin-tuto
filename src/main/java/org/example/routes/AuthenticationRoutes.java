package org.example.routes;

import io.javalin.Javalin;
import org.example.controllers.AuthController;
import org.example.middlewares.AuthMiddleware;

public class AuthenticationRoutes implements RouteHandler {
    private final AuthController authController;
    private final AuthMiddleware authMiddleware;

    public AuthenticationRoutes(AuthController authController, AuthMiddleware authMiddleware) {
        this.authController = authController;
        this.authMiddleware = authMiddleware;
    }

    @Override
    public void register(Javalin app) {
        app.post("/api/auth/login", authController::login);

        app.before("/api/auth/logout", authMiddleware.requireAuth());
        app.post("/api/auth/logout", authController::logout);
    }
}