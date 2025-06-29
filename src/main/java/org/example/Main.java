package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.example.config.DatabaseConfig;
import org.example.config.ExceptionHandlerConfig;
import org.example.controllers.AuthController;
import org.example.controllers.UserController;
import org.example.daos.UserDAO;
import org.example.mappers.UserMapper;
import org.example.middlewares.AuthMiddleware;
import org.example.repositories.UserRepository;
import org.example.repositories.impl.UserRepositoryImpl;
import org.example.routes.AuthenticationRoutes;
import org.example.routes.RouteHandler;
import org.example.routes.UserRoutes;
import org.example.services.AuthService;
import org.example.services.UserService;

import java.util.List;

public class Main {

    public static boolean isTesting = false;

    public static void main(String[] args) {
        DatabaseConfig.init();
        Javalin app = configureAndStartApp();
        
        if (!isTesting) {
            setupShutdownHook(app);
        }

        System.out.println("Server started on http://localhost:8080");
        System.out.println("Initial admin user: ******** / **********");
    }
    
    public static Javalin configureAndStartApp() {
        // Inyección de Dependencias
        final UserDAO userDAO = new UserDAO();
        final UserRepository userRepository = new UserRepositoryImpl(userDAO);
        final UserMapper userMapper = new UserMapper();
        final AuthService authService = new AuthService(userRepository, userMapper);
        final UserService userService = new UserService(userRepository, userMapper);
        final AuthMiddleware authMiddleware = new AuthMiddleware();
        final AuthController authController = new AuthController(authService);
        final UserController userController = new UserController(userService);
        final List<RouteHandler> routeHandlers = List.of(
                new AuthenticationRoutes(authController, authMiddleware),
                new UserRoutes(userController, authMiddleware)
        );

        // Configuración de Javalin
        ObjectMapper jacksonMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(jacksonMapper, false));
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> {
                it.reflectClientOrigin = true;
                it.allowCredentials = true;
                it.exposeHeader("Authorization");
            }));

        });

        routeHandlers.forEach(handler -> handler.register(app));
        ExceptionHandlerConfig.register(app);

        return app.start(8080);
    }

    private static void setupShutdownHook(Javalin app) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Closing application via shutdown hook...");
            DatabaseConfig.close();
            app.stop();
        }));
    }
}