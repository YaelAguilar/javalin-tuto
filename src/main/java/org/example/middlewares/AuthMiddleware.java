package org.example.middlewares;

import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import org.example.exceptions.UnauthorizedException;
import org.example.models.Role;
import org.example.utils.JWTUtil;

public class AuthMiddleware {

    /**
     * Middleware que verifica si un token es válido y pone los datos del usuario en el contexto.
     */
    public Handler requireAuth() {
        return ctx -> {
            String authHeader = ctx.header("Authorization");
            String token = JWTUtil.extractTokenFromHeader(authHeader);

            if (token == null) {
                throw new UnauthorizedException("Authorization token required. Format must be 'Bearer <token>'.");
            }
            if (!JWTUtil.isTokenValid(token)) {
                throw new UnauthorizedException("The provided token is invalid or has expired.");
            }
            
            Integer userId = JWTUtil.extractUserId(token);
            Role userRole = JWTUtil.extractUserRole(token);

            if (userId == null || userRole == null) {
                throw new UnauthorizedException("Invalid token: could not extract user details.");
            }

            ctx.attribute("userId", userId);
            ctx.attribute("userRole", userRole);
        };
    }

    /**
     * Middleware que verifica si el usuario tiene el rol de ADMIN.
     * DEBE ejecutarse después de requireAuth().
     */
    public Handler requireAdmin() {
        return ctx -> {
            Role userRole = ctx.attribute("userRole");
            if (userRole != Role.ADMIN) {
                throw new ForbiddenResponse("Access Denied: Administrator role required.");
            }
        };
    }
}