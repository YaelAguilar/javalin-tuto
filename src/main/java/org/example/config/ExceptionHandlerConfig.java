package org.example.config;

import io.javalin.Javalin;
import org.example.exceptions.ApiBaseException;
import org.example.exceptions.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class ExceptionHandlerConfig {

    // Instancia estática del logger para esta clase
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlerConfig.class);

    public static void register(Javalin app) {
        // Maneja nuestras excepciones personalizadas
        app.exception(ApiBaseException.class, (e, ctx) ->
                ctx.status(e.getStatusCode()).json(Map.of("success", false, "message", e.getMessage()))
        );

        // Maneja errores de validación de Javalin
        app.exception(io.javalin.http.BadRequestResponse.class, (e, ctx) ->
                ctx.status(400).json(Map.of("success", false, "message", "Petición incorrecta: " + e.getMessage()))
        );

        // Maneja errores de la capa de acceso a datos
        app.exception(DataAccessException.class, (e, ctx) -> {
            logger.error("Error de acceso a datos: {}", e.getMessage(), e);
            ctx.status(500).json(Map.of("success", false, "message", "Error interno del servidor al procesar la solicitud."));
        });

        // Maneja errores de estado inesperados en la lógica de la aplicación
        app.exception(IllegalStateException.class, (e, ctx) -> {
            logger.error("Error de estado interno: {}", e.getMessage(), e);
            ctx.status(500).json(Map.of("success", false, "message", "Error interno del servidor: " + e.getMessage()));
        });

        // Manejador "catch-all" para cualquier otra excepción no controlada
        app.exception(Exception.class, (e, ctx) -> {
            logger.error("Error no controlado ({}): {}", e.getClass().getName(), e.getMessage(), e);
            ctx.status(500).json(Map.of("success", false, "message", "Error interno del servidor."));
        });

        // Maneja errores 404 (Not Found) para endpoints que no existen
        app.error(404, ctx -> {
            if (ctx.result() == null) {
                logger.warn("Se intentó acceder a un endpoint no encontrado: {} {}", ctx.method(), ctx.path());
                ctx.json(Map.of("success", false, "message", "Endpoint no encontrado: " + ctx.method() + " " + ctx.path()));
            }
        });
    }
}