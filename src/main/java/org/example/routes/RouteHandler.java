package org.example.routes;

import io.javalin.Javalin;

@FunctionalInterface
public interface RouteHandler {
    void register(Javalin app);
}