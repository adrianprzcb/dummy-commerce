package com.adrianperezcobo.dummycommerce.orders.order.adapter.in.web;

import com.adrianperezcobo.dummycommerce.orders.order.adapter.in.web.request.CreateOrderRequest;
import com.adrianperezcobo.dummycommerce.orders.order.adapter.in.web.response.OrderResponse;
import com.adrianperezcobo.dummycommerce.orders.order.application.port.in.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final GetUserOrdersUseCase getUserOrdersUseCase;
    private final MarkStockReservedUseCase markStockReservedUseCase;
    private final MarkPaymentCompletedUseCase markPaymentCompletedUseCase;
    private final ConfirmOrderUseCase confirmOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final OrderWebMapper mapper;

    public OrderController(
            CreateOrderUseCase createOrderUseCase,
            GetOrderUseCase getOrderUseCase,
            GetUserOrdersUseCase getUserOrdersUseCase,
            MarkStockReservedUseCase markStockReservedUseCase,
            MarkPaymentCompletedUseCase markPaymentCompletedUseCase,
            ConfirmOrderUseCase confirmOrderUseCase,
            CancelOrderUseCase cancelOrderUseCase,
            OrderWebMapper mapper
    ) {
        this.createOrderUseCase = createOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.getUserOrdersUseCase = getUserOrdersUseCase;
        this.markStockReservedUseCase = markStockReservedUseCase;
        this.markPaymentCompletedUseCase = markPaymentCompletedUseCase;
        this.confirmOrderUseCase = confirmOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(
            @Valid
            @RequestBody
            CreateOrderRequest request
    ) {
        return mapper.toResponse(
                createOrderUseCase.create(
                        mapper.toCommand(request)
                )
        );
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(
            @PathVariable UUID orderId
    ) {
        return mapper.toResponse(
                getOrderUseCase.getById(orderId)
        );
    }

    @GetMapping("/user/{userId}")
    public List<OrderResponse> getByUser(
            @PathVariable UUID userId
    ) {
        return getUserOrdersUseCase
                .getByUserId(userId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @PatchMapping("/{orderId}/stock-reserved")
    public OrderResponse markStockReserved(
            @PathVariable UUID orderId
    ) {
        return mapper.toResponse(
                markStockReservedUseCase
                        .markStockReserved(orderId)
        );
    }

    @PatchMapping("/{orderId}/payment-completed")
    public OrderResponse markPaymentCompleted(
            @PathVariable UUID orderId
    ) {
        return mapper.toResponse(
                markPaymentCompletedUseCase
                        .markPaymentCompleted(orderId)
        );
    }

    @PatchMapping("/{orderId}/confirm")
    public OrderResponse confirm(
            @PathVariable UUID orderId
    ) {
        return mapper.toResponse(
                confirmOrderUseCase.confirm(orderId)
        );
    }

    @PatchMapping("/{orderId}/cancel")
    public OrderResponse cancel(
            @PathVariable UUID orderId
    ) {
        return mapper.toResponse(
                cancelOrderUseCase.cancel(orderId)
        );
    }
}