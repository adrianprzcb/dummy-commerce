package com.adrianperezcobo.dummycommerce.users.auth.adapter.out.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.refresh-token")
public record RefreshTokenProperties(
        long expirationSeconds
) {
}