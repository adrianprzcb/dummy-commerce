package com.adrianperezcobo.dummycommerce.users.auth.application.result;

public record LoginResult(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {
}