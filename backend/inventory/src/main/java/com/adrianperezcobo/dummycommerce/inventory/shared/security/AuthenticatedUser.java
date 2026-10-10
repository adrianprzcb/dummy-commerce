package com.adrianperezcobo.dummycommerce.inventory.shared.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, String role) {
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
