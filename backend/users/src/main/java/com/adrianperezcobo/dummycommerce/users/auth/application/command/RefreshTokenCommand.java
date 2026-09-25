package com.adrianperezcobo.dummycommerce.users.auth.application.command;

public record RefreshTokenCommand(
        String refreshToken
) {
}