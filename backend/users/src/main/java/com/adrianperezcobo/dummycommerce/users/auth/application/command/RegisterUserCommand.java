package com.adrianperezcobo.dummycommerce.users.auth.application.command;

public record RegisterUserCommand(
        String email,
        String password
) {
}