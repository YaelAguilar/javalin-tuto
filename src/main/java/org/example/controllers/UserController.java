package org.example.controllers;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import org.example.dtos.auth.RegisterRequest;
import org.example.models.Role;
import org.example.services.UserService;

import java.util.Map;

public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public void register(Context ctx) {
        RegisterRequest registerRequest = ctx.bodyAsClass(RegisterRequest.class);
        var newUser = userService.registerWaiter(registerRequest);
        ctx.status(201).json(Map.of(
                "success", true,
                "message", "Waiter registered successfully",
                "data", newUser
        ));
    }

    public void getAllUsers(Context ctx) {
        var users = userService.findAllUsers();
        ctx.status(200).json(Map.of(
                "success", true,
                "data", users
        ));
    }

    public void getUserById(Context ctx) {
        int requestedId = Integer.parseInt(ctx.pathParam("id"));
        int requesterId = ctx.attribute("userId");
        Role requesterRole = ctx.attribute("userRole");

        if (requesterRole != Role.ADMIN && requesterId != requestedId) {
            throw new ForbiddenResponse("You are not allowed to view this user's data.");
        }

        var user = userService.findUserById(requestedId);
        ctx.status(200).json(Map.of(
                "success", true,
                "data", user
        ));
    }

    public void getProfile(Context ctx) {
        int userId = ctx.attribute("userId");
        var userProfile = userService.findUserById(userId);
        ctx.status(200).json(Map.of(
                "success", true,
                "message", "Profile data retrieved successfully",
                "data", userProfile
        ));
    }
}