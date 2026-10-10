package com.adrianperezcobo.dummycommerce.store.shared.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, String role) {
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
