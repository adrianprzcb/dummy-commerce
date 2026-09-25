package com.adrianperezcobo.dummycommerce.users.user.adapter.in.web.response;

import com.adrianperezcobo.dummycommerce.users.user.domain.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}