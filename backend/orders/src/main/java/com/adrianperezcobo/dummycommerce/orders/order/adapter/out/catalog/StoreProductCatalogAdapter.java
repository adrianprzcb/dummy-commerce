package com.adrianperezcobo.dummycommerce.orders.order.adapter.out.catalog;

import com.adrianperezcobo.dummycommerce.orders.order.application.port.out.ProductCatalogPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

@Component
public class StoreProductCatalogAdapter implements ProductCatalogPort {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProductView(UUID id, BigDecimal price, String status) {}
    private final RestClient client;
    private final JsonMapper json;

    public StoreProductCatalogAdapter(JsonMapper json,
            @Value("${catalog.store.base-url}") String baseUrl,
            @Value("${catalog.store.connect-timeout-ms:3000}") long connectTimeout,
            @Value("${catalog.store.read-timeout-ms:5000}") long readTimeout) {
        if (connectTimeout < 1 || readTimeout < 1) throw new IllegalArgumentException("Invalid catalogue timeout");
        this.json = json;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeout)).build());
        factory.setReadTimeout(Duration.ofMillis(readTimeout));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public Optional<BigDecimal> findActivePrice(UUID productId) {
        try {
            String response = client.get().uri("/api/products/{id}", productId).retrieve().body(String.class);
            ProductView product = json.readValue(response, ProductView.class);
            if (!productId.equals(product.id()) || product.price() == null || product.price().signum() < 0 || product.status() == null) {
                throw new IllegalStateException("Invalid catalogue response");
            }
            return "ACTIVE".equals(product.status()) ? Optional.of(product.price()) : Optional.empty();
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Product catalogue is unavailable", ex);
        }
    }
}
