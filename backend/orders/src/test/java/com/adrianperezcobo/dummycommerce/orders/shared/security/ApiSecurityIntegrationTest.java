package com.adrianperezcobo.dummycommerce.orders.shared.security;

import io.jsonwebtoken.Jwts;
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
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.adrianperezcobo.dummycommerce.orders.order.application.port.out.ProductCatalogPort catalog;
    @org.junit.jupiter.api.BeforeEach
    void catalogPrice() {
        org.mockito.Mockito.when(catalog.findActivePrice(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(new java.math.BigDecimal("1.20")));
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @Value("${security.jwt.secret}") String secret;

    @Test
    void missingJwtIsUnauthorized() throws Exception {
        mvc.perform(get("/api/orders/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtIsUnauthorized() throws Exception {
        mvc.perform(get("/api/orders/me").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotInvokeAdministrativeOperation() throws Exception {
        mvc.perform(patch("/api/orders/00000000-0000-0000-0000-000000000001/confirm").header("Authorization", bearer(UUID.randomUUID(), "USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReachAdministrativeOperation() throws Exception {
        mvc.perform(patch("/api/orders/00000000-0000-0000-0000-000000000001/confirm").header("Authorization", bearer(UUID.randomUUID(), "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void validUserCanReachAllowedReadEndpoint() throws Exception {
        mvc.perform(get("/api/orders/me").header("Authorization", bearer(UUID.randomUUID(), "USER")))
                .andExpect(status().isOk());
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        String token = Jwts.builder().subject(UUID.randomUUID().toString()).claim("role", "USER")
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))).compact();
        mvc.perform(get("/api/orders/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    private String bearer(UUID id, String role) {
        return "Bearer " + Jwts.builder().subject(id.toString()).claim("email", "buyer@example.com").claim("role", role)
                .issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))).compact();
    }

    @Test
    void checkoutUsesJwtOwnerAndIgnoresForgedBodyIdentity() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orderId = createOrder(userId, UUID.randomUUID());
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(userId, "USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void foreignOrderIsHiddenAndUserCannotListAnotherUsersOrders() throws Exception {
        UUID owner = UUID.randomUUID(), attacker = UUID.randomUUID();
        UUID orderId = createOrder(owner, owner);
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(attacker, "USER")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/user/" + owner).header("Authorization", bearer(attacker, "USER")))
                .andExpect(status().isForbidden());
    }

    private UUID createOrder(UUID owner, UUID bodyOwner) throws Exception {
        String response = mvc.perform(post("/api/orders").header("Authorization", bearer(owner, "USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"userId":"%s","items":[{"productId":"%s","quantity":1,"unitPrice":1.20}]}
                                """.formatted(bodyOwner, UUID.randomUUID())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("id").asText());
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
    void onlyAdminCanReachExplicitDltReprocessing() throws Exception {
        String body = "{\"topic\":\"unsupported.DLT\",\"partition\":0,\"offset\":0}";
        mvc.perform(post("/api/admin/kafka/dlt/reprocess").contentType(MediaType.APPLICATION_JSON)
                        .content(body).header("Authorization", bearer(UUID.randomUUID(), "USER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/kafka/dlt/reprocess").contentType(MediaType.APPLICATION_JSON)
                        .content(body).header("Authorization", bearer(UUID.randomUUID(), "ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedIdentifierIsBadRequestWithoutInternalDetails() throws Exception {
        mvc.perform(get("/api/orders/invalid").header("Authorization", bearer(UUID.randomUUID(), "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request parameter is invalid"));
    }

    @Test
    void callerCannotChooseLowerPriceOrPurchaseInactiveProduct() throws Exception {
        String request = "{\"items\":[{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":1,\"unitPrice\":0.01}]}";
        String token = bearer(UUID.randomUUID(), "USER");
        mvc.perform(post("/api/orders").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Product price changed; refresh catalogue"));
        org.mockito.Mockito.when(catalog.findActivePrice(org.mockito.ArgumentMatchers.any())).thenReturn(java.util.Optional.empty());
        mvc.perform(post("/api/orders").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());
    }

}
