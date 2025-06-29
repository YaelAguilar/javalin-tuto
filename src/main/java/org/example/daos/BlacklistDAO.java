package org.example.daos;

import org.example.config.DatabaseConfig;
import org.example.exceptions.DataAccessException;
import org.intellij.lang.annotations.Language;

import java.sql.*;

public class BlacklistDAO {

    public void save(String token, Timestamp expiryDate) {
        @Language("MySQL")
        String sql = "INSERT INTO jwt_blacklist (token, expiry_date) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, token);
            pstmt.setTimestamp(2, expiryDate);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error saving token to blacklist", e);
        }
    }

    public boolean exists(String token) {
        @Language("MySQL")
        String sql = "SELECT COUNT(*) FROM jwt_blacklist WHERE token = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, token);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error checking token in blacklist", e);
        }
    }
}