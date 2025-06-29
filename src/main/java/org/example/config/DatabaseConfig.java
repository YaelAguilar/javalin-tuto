package org.example.config;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.models.Role;
import org.intellij.lang.annotations.Language;

import java.sql.*;

public class DatabaseConfig {
    private static HikariDataSource dataSource;

    public static void init() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }
        try {
            HikariConfig config = createHikariConfig();
            dataSource = new HikariDataSource(config);
            initDatabaseSchema();
        } catch (Exception e) {
            System.err.println("Error fatal al inicializar la base de datos: " + e.getMessage());
            throw new RuntimeException("No se pudo inicializar la conexión con la base de datos.", e);
        }
    }

    private static HikariConfig createHikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(AppConfig.getDbUrl());
        config.setUsername(AppConfig.getDbUser());
        config.setPassword(AppConfig.getDbPassword());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return config;
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("El pool de conexiones (DataSource) no ha sido inicializado o está cerrado.");
        }
        return dataSource.getConnection();
    }

    private static void initDatabaseSchema() {
        @Language("MySQL")
        String createUsersTableSQL = "CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, first_name VARCHAR(100) NOT NULL, middle_name VARCHAR(100), last_name VARCHAR(100) NOT NULL, email VARCHAR(255) UNIQUE NOT NULL, password VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, INDEX idx_email (email)) ENGINE=InnoDB;";
        @Language("MySQL")
        String createBlacklistTableSQL = "CREATE TABLE IF NOT EXISTS jwt_blacklist (id INT AUTO_INCREMENT PRIMARY KEY, token TEXT NOT NULL, expiry_date TIMESTAMP NOT NULL) ENGINE=InnoDB;";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTableSQL);
            stmt.execute(createBlacklistTableSQL);
            createInitialAdminUser(conn);
        } catch (SQLException e) {
            System.err.println("Error al inicializar el esquema de la base de datos: " + e.getMessage());
            throw new RuntimeException("Error durante la inicialización de la BD.", e);
        }
    }

    private static void createInitialAdminUser(Connection conn) throws SQLException {
        String checkUserSQL = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkUserSQL)) {
            checkStmt.setString(1, "admin@system.com");
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String hashedPassword = BCrypt.withDefaults().hashToString(12, "admin123".toCharArray());
                    @Language("MySQL")
                    String insertUserSQL = "INSERT INTO users (first_name, last_name, email, password, role) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertUserSQL)) {
                        insertStmt.setString(1, "System");
                        insertStmt.setString(2, "Administrator");
                        insertStmt.setString(3, "admin@system.com");
                        insertStmt.setString(4, hashedPassword);
                        insertStmt.setString(5, Role.ADMIN.name());
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}