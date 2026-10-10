package com.adrianperezcobo.dummycommerce.orders.order.adapter.in.web;

import com.adrianperezcobo.dummycommerce.orders.order.application.command.CreateOrderCommand;
import com.adrianperezcobo.dummycommerce.orders.order.application.exception.OrderNotFoundException;
import com.adrianperezcobo.dummycommerce.orders.order.application.port.in.CancelOrderUseCase;
import com.adrianperezcobo.dummycommerce.orders.order.application.port.in.ConfirmOrderUseCase;
import com.adrianperezcobo.dummycommerce.orders.order.application.port.in.CreateOrderUseCase;
import com.adrianperezcobo.dummycommerce.orders.order.application.port.in.GetOrderUseCase;
import com.adrianperezcobo.dummycommerce.orders.order.application.port.in.GetUserOrdersUseCase;
import com.adrianperezcobo.dummycommerce.orders.order.application.port.in.MarkPaymentCompletedUseCase;
import com.adrianperezcobo.dummycommerce.orders.order.application.port.in.MarkStockReservedUseCase;
import com.adrianperezcobo.dummycommerce.orders.order.domain.Order;
import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderItem;
import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderStatus;
import com.adrianperezcobo.dummycommerce.orders.order.domain.exception.InvalidOrderStateException;
import com.adrianperezcobo.dummycommerce.orders.shared.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import com.adrianperezcobo.dummycommerce.orders.shared.security.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        OrderWebMapper.class,
        GlobalExceptionHandler.class
})
class OrderControllerTest {

    @MockitoBean
    private com.adrianperezcobo.dummycommerce.orders.order.application.port.out.ProductCatalogPort catalog;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;
    @MockitoBean
    private JwtTokenValidator jwtValidator;

    @BeforeEach
    void authenticateAdmin() {
        org.mockito.Mockito.lenient().when(catalog.findActivePrice(any())).thenReturn(java.util.Optional.of(new BigDecimal("10.00")));
        org.mockito.Mockito.when(jwtValidator.parse("ADMIN_TOKEN")).thenReturn(
                new AuthenticatedUser(java.util.UUID.randomUUID(), "admin@example.com", "ADMIN"));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
                        .header("Authorization", "Bearer ADMIN_TOKEN")).build();
    }

    @MockitoBean
    private CreateOrderUseCase createOrderUseCase;

    @MockitoBean
    private GetOrderUseCase getOrderUseCase;

    @MockitoBean
    private GetUserOrdersUseCase getUserOrdersUseCase;

    @MockitoBean
    private MarkStockReservedUseCase markStockReservedUseCase;

    @MockitoBean
    private MarkPaymentCompletedUseCase markPaymentCompletedUseCase;

    @MockitoBean
    private ConfirmOrderUseCase confirmOrderUseCase;

    @MockitoBean
    private CancelOrderUseCase cancelOrderUseCase;

    @Test
    void shouldCreateOrder() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(catalog.findActivePrice(productId))
                .thenReturn(java.util.Optional.of(new BigDecimal("19.99")));

        Order order = new Order(
                UUID.randomUUID(),
                userId,
                List.of(
                        new OrderItem(
                                UUID.randomUUID(),
                                productId,
                                2,
                                new BigDecimal("19.99")
                        )
                ),
                OrderStatus.CREATED,
                Instant.now()
        );

        when(createOrderUseCase.create(
                any(CreateOrderCommand.class)
        )).thenReturn(order);

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "userId": "%s",
                                          "items": [
                                            {
                                              "productId": "%s",
                                              "quantity": 2,
                                              "unitPrice": 19.99
                                            }
                                          ]
                                        }
                                        """.formatted(
                                        userId,
                                        productId
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        order.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(userId.toString())
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CREATED")
                )
                .andExpect(
                        jsonPath("$.items[0].productId")
                                .value(
                                        productId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.items[0].quantity")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.items[0].unitPrice")
                                .value(19.99)
                )
                .andExpect(
                        jsonPath("$.items[0].subtotal")
                                .value(39.98)
                )
                .andExpect(
                        jsonPath("$.totalAmount")
                                .value(39.98)
                );
    }

    @Test
    void shouldRejectEmptyItems() throws Exception {
        mockMvc.perform(
                        post("/api/orders")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "userId": "%s",
                                          "items": []
                                        }
                                        """.formatted(
                                        UUID.randomUUID()
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.title")
                                .value("Validation error")
                )
                .andExpect(
                        jsonPath("$.errors.items")
                                .exists()
                );
    }

    @Test
    void shouldRejectInvalidNestedItem()
            throws Exception {

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "userId": "%s",
                                          "items": [
                                            {
                                              "productId": "%s",
                                              "quantity": 0,
                                              "unitPrice": 19.99
                                            }
                                          ]
                                        }
                                        """.formatted(
                                        UUID.randomUUID(),
                                        UUID.randomUUID()
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.title")
                                .value("Validation error")
                )
                .andExpect(
                        jsonPath(
                                "$.errors['items[0].quantity']"
                        ).exists()
                );
    }

    @Test
    void shouldRejectNegativePrice()
            throws Exception {

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "userId": "%s",
                                          "items": [
                                            {
                                              "productId": "%s",
                                              "quantity": 1,
                                              "unitPrice": -0.01
                                            }
                                          ]
                                        }
                                        """.formatted(
                                        UUID.randomUUID(),
                                        UUID.randomUUID()
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.errors['items[0].unitPrice']"
                        ).exists()
                );
    }

    @Test
    void shouldGetOrder() throws Exception {
        Order order = createOrder(
                OrderStatus.CREATED
        );

        when(getOrderUseCase.getById(
                order.getId()
        )).thenReturn(order);

        mockMvc.perform(
                        get(
                                "/api/orders/{orderId}",
                                order.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        order.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CREATED")
                )
                .andExpect(
                        jsonPath("$.totalAmount")
                                .value(10.00)
                );
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        when(getOrderUseCase.getById(orderId))
                .thenThrow(
                        new OrderNotFoundException(
                                orderId
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/orders/{orderId}",
                                orderId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Order not found")
                );
    }

    @Test
    void shouldGetOrdersByUser()
            throws Exception {

        UUID userId = UUID.randomUUID();

        Order first = createOrder(
                userId,
                OrderStatus.CREATED
        );

        Order second = createOrder(
                userId,
                OrderStatus.CONFIRMED
        );

        when(getUserOrdersUseCase
                .getByUserId(userId))
                .thenReturn(
                        List.of(first, second)
                );

        mockMvc.perform(
                        get(
                                "/api/orders/user/{userId}",
                                userId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].userId")
                                .value(userId.toString())
                )
                .andExpect(
                        jsonPath("$[1].status")
                                .value("CONFIRMED")
                );
    }

    @Test
    void shouldMarkStockReserved()
            throws Exception {

        Order order = createOrder(
                OrderStatus.CREATED
        );

        order.markStockReserved();

        when(markStockReservedUseCase
                .markStockReserved(
                        order.getId()
                ))
                .thenReturn(order);

        mockMvc.perform(
                        patch(
                                "/api/orders/{orderId}/stock-reserved",
                                order.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value(
                                        "STOCK_RESERVED"
                                )
                );
    }

    @Test
    void shouldMarkPaymentCompleted()
            throws Exception {

        Order order = createOrder(
                OrderStatus.STOCK_RESERVED
        );

        order.markPaymentCompleted();

        when(markPaymentCompletedUseCase
                .markPaymentCompleted(
                        order.getId()
                ))
                .thenReturn(order);

        mockMvc.perform(
                        patch(
                                "/api/orders/{orderId}/payment-completed",
                                order.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value(
                                        "PAYMENT_COMPLETED"
                                )
                );
    }

    @Test
    void shouldConfirmOrder()
            throws Exception {

        Order order = createOrder(
                OrderStatus.PAYMENT_COMPLETED
        );

        order.confirm();

        when(confirmOrderUseCase.confirm(
                order.getId()
        )).thenReturn(order);

        mockMvc.perform(
                        patch(
                                "/api/orders/{orderId}/confirm",
                                order.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("CONFIRMED")
                );
    }

    @Test
    void shouldCancelOrder()
            throws Exception {

        Order order = createOrder(
                OrderStatus.STOCK_RESERVED
        );

        order.cancel();

        when(cancelOrderUseCase.cancel(
                order.getId()
        )).thenReturn(order);

        mockMvc.perform(
                        patch(
                                "/api/orders/{orderId}/cancel",
                                order.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("CANCELLED")
                );
    }

    @Test
    void shouldReturnConflictForInvalidTransition()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        when(confirmOrderUseCase.confirm(
                orderId
        )).thenThrow(
                new InvalidOrderStateException(
                        "Order must be PAYMENT_COMPLETED but is CREATED"
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/orders/{orderId}/confirm",
                                orderId
                        )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Invalid order state"
                                )
                );
    }

    private Order createOrder(
            OrderStatus status
    ) {
        return createOrder(
                UUID.randomUUID(),
                status
        );
    }

    private Order createOrder(
            UUID userId,
            OrderStatus status
    ) {
        return new Order(
                UUID.randomUUID(),
                userId,
                List.of(
                        new OrderItem(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                1,
                                new BigDecimal("10.00")
                        )
                ),
                status,
                Instant.now()
        );
    }
}
