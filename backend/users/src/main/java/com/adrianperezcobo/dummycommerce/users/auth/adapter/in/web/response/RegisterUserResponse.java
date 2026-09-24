package com.adrianperezcobo.dummycommerce.users.auth.adapter.in.web.response;

import com.adrianperezcobo.dummycommerce.users.user.domain.Role;

import java.time.Instant;
import java.util.UUID;

public record RegisterUserResponse(
        UUID id,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}