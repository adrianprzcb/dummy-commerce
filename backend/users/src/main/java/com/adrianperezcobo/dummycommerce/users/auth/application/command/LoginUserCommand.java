package com.adrianperezcobo.dummycommerce.users.auth.application.command;

public record LoginUserCommand(
        String email,
        String password
) {
}