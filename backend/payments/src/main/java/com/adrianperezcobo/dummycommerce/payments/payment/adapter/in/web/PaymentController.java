package com.adrianperezcobo.dummycommerce.payments.payment.adapter.in.web;

import com.adrianperezcobo.dummycommerce.payments.payment.adapter.in.web.request.ProcessPaymentRequest;
import com.adrianperezcobo.dummycommerce.payments.payment.adapter.in.web.response.PaymentResponse;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.in.GetPaymentByOrderUseCase;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.in.GetPaymentUseCase;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.in.ProcessPaymentUseCase;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.in.RefundPaymentUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final GetPaymentByOrderUseCase getPaymentByOrderUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;
    private final PaymentWebMapper mapper;

    public PaymentController(
            ProcessPaymentUseCase processPaymentUseCase,
            GetPaymentUseCase getPaymentUseCase,
            GetPaymentByOrderUseCase getPaymentByOrderUseCase,
            RefundPaymentUseCase refundPaymentUseCase,
            PaymentWebMapper mapper
    ) {
        this.processPaymentUseCase =
                processPaymentUseCase;

        this.getPaymentUseCase =
                getPaymentUseCase;

        this.getPaymentByOrderUseCase =
                getPaymentByOrderUseCase;

        this.refundPaymentUseCase =
                refundPaymentUseCase;

        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse process(
            @Valid
            @RequestBody
            ProcessPaymentRequest request
    ) {
        return mapper.toResponse(
                processPaymentUseCase.process(
                        mapper.toCommand(request)
                )
        );
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getById(
            @PathVariable UUID paymentId
    ) {
        return mapper.toResponse(
                getPaymentUseCase.getById(
                        paymentId
                )
        );
    }

    @GetMapping("/order/{orderId}")
    public PaymentResponse getByOrderId(
            @PathVariable UUID orderId
    ) {
        return mapper.toResponse(
                getPaymentByOrderUseCase
                        .getByOrderId(orderId)
        );
    }

    @PostMapping("/{paymentId}/refund")
    public PaymentResponse refund(
            @PathVariable UUID paymentId
    ) {
        return mapper.toResponse(
                refundPaymentUseCase.refund(
                        paymentId
                )
        );
    }
}