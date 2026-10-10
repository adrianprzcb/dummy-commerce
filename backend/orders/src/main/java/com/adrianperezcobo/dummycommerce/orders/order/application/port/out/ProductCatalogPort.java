package com.adrianperezcobo.dummycommerce.orders.order.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ProductCatalogPort {
    Optional<BigDecimal> findActivePrice(UUID productId);
}
