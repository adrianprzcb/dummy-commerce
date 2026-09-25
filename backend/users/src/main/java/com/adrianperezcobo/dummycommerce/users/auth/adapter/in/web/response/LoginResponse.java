package com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}