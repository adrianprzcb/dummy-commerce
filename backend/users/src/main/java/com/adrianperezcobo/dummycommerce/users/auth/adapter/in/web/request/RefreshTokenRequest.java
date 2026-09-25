package com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(

        @NotBlank
        String refreshToken
) {
}