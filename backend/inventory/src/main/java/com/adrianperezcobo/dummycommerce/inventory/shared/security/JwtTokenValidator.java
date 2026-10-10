package com.adrianperezcobo.dummycommerce.inventory.shared.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.UUID;

@Component
public class JwtTokenValidator {
    private final SecretKey key;

    public JwtTokenValidator(@Value("${security.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        String role = claims.get("role", String.class);
        if ((!"USER".equals(role) && !"ADMIN".equals(role)) || claims.getExpiration() == null) {
            throw new IllegalArgumentException("Invalid access token claims");
        }
        return new AuthenticatedUser(UUID.fromString(claims.getSubject()), claims.get("email", String.class), role);
    }
}
