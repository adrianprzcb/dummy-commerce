package com.adrianperezcobo.dummycommerce.inventory.reservation.application.integration.inventory;

public final class InventoryTopics {
    public static final String RESERVE_STOCK_V1 = "dummy-commerce.inventory.reserve-stock.v1";
    public static final String STOCK_RESERVED_V1 = "dummy-commerce.inventory.stock-reserved.v1";
    public static final String STOCK_RESERVATION_FAILED_V1 = "dummy-commerce.inventory.stock-reservation-failed.v1";
    public static final String CONFIRM_STOCK_V1 = "dummy-commerce.inventory.confirm-stock.v1";
    public static final String STOCK_CONFIRMED_V1 = "dummy-commerce.inventory.stock-confirmed.v1";
    public static final String RELEASE_STOCK_V1 = "dummy-commerce.inventory.release-stock.v1";
    public static final String STOCK_RELEASED_V1 = "dummy-commerce.inventory.stock-released.v1";

    public static final String STOCK_CONFIRMATION_FAILED_V1 = "dummy-commerce.inventory.stock-confirmation-failed.v1";

    private InventoryTopics() { }
}
