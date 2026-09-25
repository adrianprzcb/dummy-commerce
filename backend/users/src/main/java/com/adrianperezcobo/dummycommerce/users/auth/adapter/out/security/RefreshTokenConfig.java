package com.adrianperezcobo.dummycommerce.users.auth.adapter.out.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(
        RefreshTokenProperties.class
)
public class RefreshTokenConfig {
}