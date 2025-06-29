package org.example.models;

import io.javalin.security.RouteRole;

public enum Role implements RouteRole {
    ADMIN,
    WAITER,
    ANYONE
}