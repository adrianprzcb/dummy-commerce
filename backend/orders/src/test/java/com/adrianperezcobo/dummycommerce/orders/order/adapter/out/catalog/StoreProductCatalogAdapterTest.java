package com.adrianperezcobo.dummycommerce.orders.order.adapter.out.catalog;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class StoreProductCatalogAdapterTest {
    HttpServer server;
    StoreProductCatalogAdapter catalog;
    UUID productId = UUID.randomUUID();
    int status;
    String body;

    @BeforeEach
    void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/products/", exchange -> {
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        catalog = new StoreProductCatalogAdapter(JsonMapper.builder().build(), "http://localhost:" + server.getAddress().getPort(), 1000, 1000);
    }

    @AfterEach
    void cleanup() { server.stop(0); }

    @Test
    void activeProductPriceRetainsDecimalPrecision() {
        status = 200;
        body = "{\"id\":\"" + productId + "\",\"price\":9007199254740991.99,\"status\":\"ACTIVE\",\"name\":\"Product\"}";
        assertThat(catalog.findActivePrice(productId)).contains(new BigDecimal("9007199254740991.99"));
    }

    @Test
    void missingOrInactiveProductCannotBePurchased() {
        status = 404; body = "{}";
        assertThat(catalog.findActivePrice(productId)).isEmpty();
        status = 200;
        body = "{\"id\":\"" + productId + "\",\"price\":12.34,\"status\":\"DRAFT\"}";
        assertThat(catalog.findActivePrice(productId)).isEmpty();
    }

    @Test
    void technicalFailureIsNotReturnedAsProductNotFound() {
        status = 503; body = "internal failure";
        assertThatThrownBy(() -> catalog.findActivePrice(productId)).isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(502));
    }
}
