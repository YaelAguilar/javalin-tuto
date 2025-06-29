package org.example.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.example.config.AppConfig;
import org.example.daos.BlacklistDAO;
import org.example.models.Role;
import org.example.models.User;

import javax.crypto.SecretKey;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Date;

public class JWTUtil {

    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(Base64.getDecoder().decode(AppConfig.getJwtSecretKey()));

    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24 horas
    
    private static final BlacklistDAO blacklistDAO = new BlacklistDAO();

    public static String generateToken(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User details cannot be null or empty for token generation.");
        }
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(SECRET_KEY)
                .compact();
    }

    public static Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    public static Integer extractUserId(String token) {
        try {
            return Integer.parseInt(extractAllClaims(token).getSubject());
        } catch (JwtException | NumberFormatException e) {
            return null;
        }
    }

    public static Role extractUserRole(String token) {
        try {
            String roleStr = extractAllClaims(token).get("role", String.class);
            return Role.valueOf(roleStr);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isTokenValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
            return !isTokenBlacklisted(token);
        } catch (ExpiredJwtException e) {
            System.err.println("JWT token is expired: " + e.getMessage());
            return false;
        } catch (SignatureException e) {
            System.err.println("Invalid JWT signature: " + e.getMessage());
            return false;
        } catch (JwtException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
            return false;
        }
    }



    public static void blacklistToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            try {
                Date expiration = extractAllClaims(token).getExpiration();
                blacklistDAO.save(token, new Timestamp(expiration.getTime()));
            } catch (JwtException e) {
                System.err.println("Attempted to blacklist an invalid token: " + e.getMessage());
            }
        }
    }

    private static boolean isTokenBlacklisted(String token) {
        return blacklistDAO.exists(token);
    }

    public static String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }
}