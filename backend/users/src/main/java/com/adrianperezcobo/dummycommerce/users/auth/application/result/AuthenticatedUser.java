package com.adrianperezcobo.dummycommerce.users.auth.application.result;

import com.adrianperezcobo.dummycommerce.users.user.domain.Role;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String email,
        Role role
) {
}