package com.adrianperezcobo.dummycommerce.users.auth.application.result;

public record RefreshTokenResult(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {
}