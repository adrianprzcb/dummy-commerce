package com.adrianperezcobo.dummycommerce.payments.payment.adapter.in.web;

import com.adrianperezcobo.dummycommerce.payments.payment.adapter.in.web.request.ProcessPaymentRequest;
import com.adrianperezcobo.dummycommerce.payments.payment.adapter.in.web.response.PaymentResponse;
import com.adrianperezcobo.dummycommerce.payments.payment.application.command.ProcessPaymentCommand;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentWebMapper {

    public ProcessPaymentCommand toCommand(
            ProcessPaymentRequest request
    ) {
        return new ProcessPaymentCommand(
                request.orderId(),
                request.amount(),
                request.currency()
        );
    }

    public PaymentResponse toResponse(
            Payment payment
    ) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}