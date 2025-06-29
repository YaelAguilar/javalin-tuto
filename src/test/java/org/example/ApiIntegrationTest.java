package org.example;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import org.example.config.DatabaseConfig;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApiIntegrationTest {

    private static Javalin app;
    private static final String BASE_URL = "http://localhost:8080";
    private static HttpClient client;
    private static ObjectMapper objectMapper;

    private record TestResponse(int statusCode, String body) {}

    @BeforeAll
    public void setupAll() {
        Main.isTesting = true;
        DatabaseConfig.init();
        app = Main.configureAndStartApp();
        client = HttpClient.newHttpClient();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @AfterAll
    public void teardownAll() {
        app.stop();
        DatabaseConfig.close();
    }

    @BeforeEach
    public void resetDatabase() {
        String hashedPassword = BCrypt.withDefaults().hashToString(12, "admin123".toCharArray());

        // Usamos un PreparedStatement para una inserción más segura y limpia.
        String insertAdminSQL = "INSERT INTO users (id, first_name, last_name, email, password, role) VALUES (1, 'System', 'Administrator', 'admin@system.com', ?, 'ADMIN');";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             PreparedStatement pstmt = conn.prepareStatement(insertAdminSQL)) {

            // Limpiamos las tablas
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");
            stmt.execute("TRUNCATE TABLE users;");
            stmt.execute("TRUNCATE TABLE jwt_blacklist;");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1;");

            // Insertamos el admin con el hash recién generado
            pstmt.setString(1, hashedPassword);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error reseteando la base de datos: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("1. Admin debe poder loguearse y obtener un token")
    void adminLoginSuccess() throws IOException, InterruptedException {
        String loginBody = "{\"email\": \"admin@system.com\", \"password\": \"admin123\"}";
        TestResponse response = makeRequest("POST", "/api/auth/login", loginBody, null);

        assertThat(response.statusCode)
            .withFailMessage("Login de admin falló. Status: %s, Body: %s", response.statusCode, response.body)
            .isEqualTo(200);

        JsonNode responseJson = objectMapper.readTree(response.body);
        assertThat(responseJson.get("success").asBoolean()).isTrue();
        assertThat(responseJson.at("/data/token").asText()).isNotBlank();
    }

    @Test
    @DisplayName("2. Admin debe poder registrar un nuevo Mesero")
    void adminCanRegisterWaiter() throws IOException, InterruptedException {
        String adminToken = getAdminToken();
        String newWaiterBody = "{\"firstName\": \"Pedro\", \"lastName\": \"Paramo\", \"email\": \"pedro.p@restaurante.com\", \"password\": \"rulfopass1\", \"confirmPassword\": \"rulfopass1\"}";
        
        TestResponse response = makeRequest("POST", "/api/users/register", newWaiterBody, adminToken);

        assertThat(response.statusCode).isEqualTo(201);
        JsonNode responseJson = objectMapper.readTree(response.body);
        assertThat(responseJson.at("/data/email").asText()).isEqualTo("pedro.p@restaurante.com");
    }

    @Test
    @DisplayName("3. Admin debe poder ver todos los usuarios")
    void adminCanGetAllUsers() throws IOException, InterruptedException {
        String adminToken = getAdminToken();
        makeRequest("POST", "/api/users/register", "{\"firstName\": \"Test\", \"lastName\": \"User\", \"email\": \"test@user.com\", \"password\": \"pass\", \"confirmPassword\": \"pass\"}", adminToken);
        
        TestResponse response = makeRequest("GET", "/api/users", null, adminToken);
        
        assertThat(response.statusCode).isEqualTo(200);
        JsonNode data = objectMapper.readTree(response.body).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data).hasSize(2);
    }

    @Test
    @DisplayName("4. Mesero NO debe poder ver todos los usuarios")
    void waiterCannotGetAllUsers() throws IOException, InterruptedException {
        String adminToken = getAdminToken();
        makeRequest("POST", "/api/users/register", "{\"firstName\": \"Waiter\", \"lastName\": \"Test\", \"email\": \"waiter@test.com\", \"password\": \"pass\", \"confirmPassword\": \"pass\"}", adminToken);
        String waiterToken = getUserToken("waiter@test.com", "pass");

        TestResponse response = makeRequest("GET", "/api/users", null, waiterToken);
        assertThat(response.statusCode).isEqualTo(403);
    }
    
    @Test
    @DisplayName("5. Logout debe invalidar el token")
    void logoutInvalidatesToken() throws IOException, InterruptedException {
        String adminToken = getAdminToken();
        makeRequest("POST", "/api/users/register", "{\"firstName\": \"Logout\", \"lastName\": \"Test\", \"email\": \"logout@test.com\", \"password\": \"pass\", \"confirmPassword\": \"pass\"}", adminToken);
        String waiterToken = getUserToken("logout@test.com", "pass");

        TestResponse logoutResponse = makeRequest("POST", "/api/auth/logout", null, waiterToken);
        assertThat(logoutResponse.statusCode).isEqualTo(200);

        TestResponse profileResponse = makeRequest("GET", "/api/users/profile", null, waiterToken);
        assertThat(profileResponse.statusCode).isEqualTo(401);
    }
    
    private String getAdminToken() throws IOException, InterruptedException {
        return getUserToken("admin@system.com", "admin123");
    }

    private String getUserToken(String email, String password) throws IOException, InterruptedException {
        String loginBody = String.format("{\"email\": \"%s\", \"password\": \"%s\"}", email, password);
        TestResponse response = makeRequest("POST", "/api/auth/login", loginBody, null);
        assertThat(response.statusCode)
            .withFailMessage("Fallo al obtener token para %s. Status: %s, Body: %s", email, response.statusCode, response.body)
            .isEqualTo(200);
        return objectMapper.readTree(response.body).at("/data/token").asText();
    }

    private TestResponse makeRequest(String method, String path, String body, String token) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json");

        if (token != null && !token.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        HttpRequest.BodyPublisher bodyPublisher = (body == null || body.isEmpty()) 
            ? HttpRequest.BodyPublishers.noBody() 
            : HttpRequest.BodyPublishers.ofString(body);

        switch (method.toUpperCase()) {
            case "POST" -> requestBuilder.POST(bodyPublisher);
            case "GET" -> requestBuilder.GET();
        }

        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        return new TestResponse(response.statusCode(), response.body());
    }
}