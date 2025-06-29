package org.example.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.example.Main;

public class AppConfig {
    
    private static Dotenv dotenv;

    private static synchronized Dotenv getDotenv() {
        if (dotenv == null) {
            String filename = Main.isTesting ? ".env.test" : ".env";
            System.out.println("LAZY LOADING a partir de: " + filename);
            dotenv = Dotenv.configure()
                    .filename(filename)
                    .directory("./")
                    .ignoreIfMissing()
                    .load();
        }
        return dotenv;
    }

    public static String getDbUrl() {
        return getRequiredEnv("DB_URL");
    }

    public static String getDbUser() {
        return getRequiredEnv("DB_USER");
    }
    
    public static String getDbPassword() {
        return getRequiredEnv("DB_PASSWORD");
    }

    public static String getJwtSecretKey() {
        return getRequiredEnv("JWT_SECRET_KEY");
    }

    private static String getRequiredEnv(String key) {
        String value = getDotenv().get(key);
        
        if (value == null) {
            value = System.getenv(key);
        }
        if (value == null) {
            System.err.println("Error: La variable de entorno requerida '" + key + "' no está definida.");
            System.err.println("Asegúrate de tener el archivo .env o .env.test adecuado en la raíz del proyecto.");
            throw new RuntimeException("Variable de entorno requerida no encontrada: " + key);
        }
        return value;
    }
}