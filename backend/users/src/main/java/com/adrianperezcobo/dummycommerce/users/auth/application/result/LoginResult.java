package com.adrianperezcobo.dummycommerce.users.auth.application.result;

public record LoginResult(
        String accessToken,
        long expiresIn
) {
}