package com.adrianperezcobo.dummycommerce.store.shared.security;

import io.jsonwebtoken.Jwts;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductImageStoragePort;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApiSecurityIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @MockitoBean ProductImageStoragePort storage;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @Value("${security.jwt.secret}") String secret;

    @Test
    void missingJwtIsUnauthorized() throws Exception {
        mvc.perform(post("/api/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtIsUnauthorized() throws Exception {
        mvc.perform(post("/api/categories").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotInvokeAdministrativeOperation() throws Exception {
        mvc.perform(post("/api/categories").header("Authorization", bearer(UUID.randomUUID(), "USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReachAdministrativeOperation() throws Exception {
        mvc.perform(post("/api/categories").header("Authorization", bearer(UUID.randomUUID(), "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validUserCanReachAllowedReadEndpoint() throws Exception {
        mvc.perform(get("/api/products").header("Authorization", bearer(UUID.randomUUID(), "USER")))
                .andExpect(status().isOk());
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        String token = Jwts.builder().subject(UUID.randomUUID().toString()).claim("role", "USER")
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))).compact();
        mvc.perform(post("/api/categories").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    private String bearer(UUID id, String role) {
        return "Bearer " + Jwts.builder().subject(id.toString()).claim("email", "buyer@example.com").claim("role", role)
                .issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))).compact();
    }

    @Test
    void allowedCorsPreflightDoesNotRequireJwt() throws Exception {
        mvc.perform(options("/api/test").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void unknownCorsOriginIsRejected() throws Exception {
        mvc.perform(options("/api/test").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void malformedIdentifierIsBadRequestWithoutInternalDetails() throws Exception {
        mvc.perform(get("/api/products/invalid").header("Authorization", bearer(UUID.randomUUID(), "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request parameter is invalid"));
    }

}
