package com.adrianperezcobo.dummycommerce.payments.payment.adapter.in.web;

import com.adrianperezcobo.dummycommerce.payments.payment.application.command.ProcessPaymentCommand;
import com.adrianperezcobo.dummycommerce.payments.payment.application.exception.PaymentNotFoundException;
import com.adrianperezcobo.dummycommerce.payments.payment.application.exception.PaymentRefundFailedException;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.in.GetPaymentByOrderUseCase;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.in.GetPaymentUseCase;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.in.ProcessPaymentUseCase;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.in.RefundPaymentUseCase;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.Payment;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.PaymentStatus;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.exception.InvalidPaymentStateException;
import com.adrianperezcobo.dummycommerce.payments.shared.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import com.adrianperezcobo.dummycommerce.payments.shared.security.*;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        PaymentWebMapper.class,
        GlobalExceptionHandler.class
})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;
    @MockitoBean
    private JwtTokenValidator jwtValidator;

    @BeforeEach
    void authenticateAdmin() {
        org.mockito.Mockito.when(jwtValidator.parse("ADMIN_TOKEN")).thenReturn(
                new AuthenticatedUser(java.util.UUID.randomUUID(), "admin@example.com", "ADMIN"));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
                        .header("Authorization", "Bearer ADMIN_TOKEN")).build();
    }

    @MockitoBean
    private ProcessPaymentUseCase processPaymentUseCase;

    @MockitoBean
    private GetPaymentUseCase getPaymentUseCase;

    @MockitoBean
    private GetPaymentByOrderUseCase getPaymentByOrderUseCase;

    @MockitoBean
    private RefundPaymentUseCase refundPaymentUseCase;

    @Test
    void shouldProcessPayment() throws Exception {
        UUID orderId = UUID.randomUUID();

        Payment payment =
                payment(
                        orderId,
                        PaymentStatus.COMPLETED
                );

        when(processPaymentUseCase.process(
                any(ProcessPaymentCommand.class)
        )).thenReturn(payment);

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "orderId": "%s",
                                          "amount": 49.98,
                                          "currency": "eur"
                                        }
                                        """.formatted(orderId))
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.orderId")
                                .value(orderId.toString())
                )
                .andExpect(
                        jsonPath("$.amount")
                                .value(49.98)
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("EUR")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("COMPLETED")
                );
    }

    @Test
    void shouldRejectZeroAmount()
            throws Exception {

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "orderId": "%s",
                                          "amount": 0,
                                          "currency": "EUR"
                                        }
                                        """.formatted(
                                        UUID.randomUUID()
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.errors.amount")
                                .exists()
                );
    }

    @Test
    void shouldRejectInvalidCurrency()
            throws Exception {

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "orderId": "%s",
                                          "amount": 10.00,
                                          "currency": "EU"
                                        }
                                        """.formatted(
                                        UUID.randomUUID()
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.errors.currency")
                                .exists()
                );
    }

    @Test
    void shouldGetPaymentById()
            throws Exception {

        Payment payment =
                payment(
                        UUID.randomUUID(),
                        PaymentStatus.COMPLETED
                );

        when(getPaymentUseCase.getById(
                payment.getId()
        )).thenReturn(payment);

        mockMvc.perform(
                        get(
                                "/api/payments/{paymentId}",
                                payment.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        payment.getId()
                                                .toString()
                                )
                );
    }

    @Test
    void shouldGetPaymentByOrderId()
            throws Exception {

        Payment payment =
                payment(
                        UUID.randomUUID(),
                        PaymentStatus.COMPLETED
                );

        when(getPaymentByOrderUseCase
                .getByOrderId(
                        payment.getOrderId()
                ))
                .thenReturn(payment);

        mockMvc.perform(
                        get(
                                "/api/payments/order/{orderId}",
                                payment.getOrderId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.orderId")
                                .value(
                                        payment.getOrderId()
                                                .toString()
                                )
                );
    }

    @Test
    void shouldReturnNotFound()
            throws Exception {

        UUID paymentId = UUID.randomUUID();

        when(getPaymentUseCase.getById(
                paymentId
        )).thenThrow(
                new PaymentNotFoundException(
                        paymentId
                )
        );

        mockMvc.perform(
                        get(
                                "/api/payments/{paymentId}",
                                paymentId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Payment not found")
                );
    }

    @Test
    void shouldRefundPayment()
            throws Exception {

        Payment payment =
                payment(
                        UUID.randomUUID(),
                        PaymentStatus.COMPLETED
                );

        payment.refund(Instant.now());

        when(refundPaymentUseCase.refund(
                payment.getId()
        )).thenReturn(payment);

        mockMvc.perform(
                        post(
                                "/api/payments/{paymentId}/refund",
                                payment.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("REFUNDED")
                );
    }

    @Test
    void shouldReturnConflictForInvalidRefundState()
            throws Exception {

        UUID paymentId = UUID.randomUUID();

        when(refundPaymentUseCase.refund(
                paymentId
        )).thenThrow(
                new InvalidPaymentStateException(
                        "Payment must be COMPLETED but is FAILED"
                )
        );

        mockMvc.perform(
                        post(
                                "/api/payments/{paymentId}/refund",
                                paymentId
                        )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Invalid payment state"
                                )
                );
    }

    @Test
    void shouldReturnBadGatewayWhenRefundProcessorFails()
            throws Exception {

        UUID paymentId = UUID.randomUUID();

        when(refundPaymentUseCase.refund(
                paymentId
        )).thenThrow(
                new PaymentRefundFailedException(
                        paymentId
                )
        );

        mockMvc.perform(
                        post(
                                "/api/payments/{paymentId}/refund",
                                paymentId
                        )
                )
                .andExpect(status().isBadGateway())
                .andExpect(
                        jsonPath("$.status")
                                .value(502)
                );
    }

    private Payment payment(
            UUID orderId,
            PaymentStatus status
    ) {
        Instant now = Instant.now();

        return new Payment(
                UUID.randomUUID(),
                orderId,
                new BigDecimal("49.98"),
                "EUR",
                status,
                now,
                now
        );
    }
}