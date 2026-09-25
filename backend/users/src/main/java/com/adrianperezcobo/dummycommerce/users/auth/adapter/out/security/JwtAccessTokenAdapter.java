package com.adrianperezcobo.dummycommerce.users.auth.adapter.out.security;

import com.adrianperezcobo.dummycommerce.users.auth.application.exception.InvalidAccessTokenException;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.AccessTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.AuthenticatedUser;
import com.adrianperezcobo.dummycommerce.users.user.domain.Role;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtAccessTokenAdapter implements AccessTokenPort {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtAccessTokenAdapter(
            JwtProperties properties
    ) {
        this.properties = properties;

        this.signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(
                        properties.secret()
                )
        );
    }

    @Override
    public String generate(User user) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(
                                now.plusSeconds(
                                        properties.accessTokenExpirationSeconds()
                                )
                        )
                )
                .signWith(signingKey)
                .compact();
    }

    @Override
    public AuthenticatedUser parse(String token) {

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    claims.get("email", String.class),
                    Role.valueOf(
                            claims.get("role", String.class)
                    )
            );

        } catch (
                JwtException |
                IllegalArgumentException exception
        ) {
            throw new InvalidAccessTokenException();
        }
    }

    @Override
    public long getExpirationSeconds() {
        return properties.accessTokenExpirationSeconds();
    }
}