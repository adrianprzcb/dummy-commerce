package com.adrianperezcobo.dummycommerce.payments.payment.application.service;

import com.adrianperezcobo.dummycommerce.payments.payment.application.command.ProcessPaymentCommand;
import com.adrianperezcobo.dummycommerce.payments.payment.application.exception.PaymentForOrderNotFoundException;
import com.adrianperezcobo.dummycommerce.payments.payment.application.exception.PaymentNotFoundException;
import com.adrianperezcobo.dummycommerce.payments.payment.application.exception.PaymentRefundFailedException;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.out.PaymentProcessorPort;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.out.PaymentProcessorResult;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.out.PaymentRepository;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.Payment;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.PaymentStatus;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProcessorPort paymentProcessor;

    @InjectMocks
    private PaymentService service;

    @Test
    void shouldProcessSuccessfulPayment() {
        UUID orderId = UUID.randomUUID();

        when(paymentRepository
                .findByOrderIdForUpdate(orderId))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(paymentProcessor.process(
                any(UUID.class),
                eq(orderId),
                eq(new BigDecimal("49.98")),
                eq("EUR")
        )).thenReturn(
                PaymentProcessorResult.SUCCESS
        );

        Payment result = service.process(
                new ProcessPaymentCommand(
                        orderId,
                        new BigDecimal("49.98"),
                        "eur"
                )
        );

        assertThat(result.getStatus())
                .isEqualTo(PaymentStatus.COMPLETED);

        assertThat(result.getOrderId())
                .isEqualTo(orderId);

        assertThat(result.getCurrency())
                .isEqualTo("EUR");

        verify(paymentProcessor, times(1))
                .process(
                        any(UUID.class),
                        eq(orderId),
                        eq(new BigDecimal("49.98")),
                        eq("EUR")
                );

        verify(paymentRepository, times(2))
                .save(any(Payment.class));
    }

    @Test
    void shouldStoreFailedPaymentWhenProcessorRejectsIt() {
        UUID orderId = UUID.randomUUID();

        when(paymentRepository
                .findByOrderIdForUpdate(orderId))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(paymentProcessor.process(
                any(),
                eq(orderId),
                any(),
                eq("EUR")
        )).thenReturn(
                PaymentProcessorResult.FAILURE
        );

        Payment result = service.process(
                new ProcessPaymentCommand(
                        orderId,
                        new BigDecimal("30.00"),
                        "EUR"
                )
        );

        assertThat(result.getStatus())
                .isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void shouldReturnExistingCompletedPaymentWithoutProcessingAgain() {
        Payment existing =
                payment(PaymentStatus.COMPLETED);

        when(paymentRepository
                .findByOrderIdForUpdate(
                        existing.getOrderId()
                ))
                .thenReturn(Optional.of(existing));

        Payment result = service.process(
                commandFor(existing)
        );

        assertThat(result)
                .isSameAs(existing);

        verifyNoInteractions(paymentProcessor);

        verify(paymentRepository, never())
                .save(any());
    }

    @Test
    void shouldReturnExistingFailedPaymentWithoutProcessingAgain() {
        Payment existing =
                payment(PaymentStatus.FAILED);

        when(paymentRepository
                .findByOrderIdForUpdate(
                        existing.getOrderId()
                ))
                .thenReturn(Optional.of(existing));

        Payment result = service.process(
                commandFor(existing)
        );

        assertThat(result)
                .isSameAs(existing);

        verifyNoInteractions(paymentProcessor);
    }

    @Test
    void shouldRetryExistingPendingPayment() {
        Payment existing =
                payment(PaymentStatus.PENDING);

        when(paymentRepository
                .findByOrderIdForUpdate(
                        existing.getOrderId()
                ))
                .thenReturn(Optional.of(existing));

        when(paymentProcessor.process(
                existing.getId(),
                existing.getOrderId(),
                existing.getAmount(),
                existing.getCurrency()
        )).thenReturn(
                PaymentProcessorResult.SUCCESS
        );

        when(paymentRepository.save(existing))
                .thenReturn(existing);

        Payment result = service.process(
                commandFor(existing)
        );

        assertThat(result.getStatus())
                .isEqualTo(PaymentStatus.COMPLETED);

        verify(paymentProcessor, times(1))
                .process(
                        existing.getId(),
                        existing.getOrderId(),
                        existing.getAmount(),
                        existing.getCurrency()
                );
    }

    @Test
    void shouldGetPaymentById() {
        Payment payment =
                payment(PaymentStatus.COMPLETED);

        when(paymentRepository.findById(
                payment.getId()
        )).thenReturn(Optional.of(payment));

        assertThat(
                service.getById(payment.getId())
        ).isSameAs(payment);
    }

    @Test
    void shouldThrowWhenPaymentDoesNotExist() {
        UUID paymentId = UUID.randomUUID();

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getById(paymentId)
        ).isInstanceOf(
                PaymentNotFoundException.class
        );
    }

    @Test
    void shouldGetPaymentByOrderId() {
        Payment payment =
                payment(PaymentStatus.COMPLETED);

        when(paymentRepository.findByOrderId(
                payment.getOrderId()
        )).thenReturn(Optional.of(payment));

        assertThat(
                service.getByOrderId(
                        payment.getOrderId()
                )
        ).isSameAs(payment);
    }

    @Test
    void shouldThrowWhenOrderHasNoPayment() {
        UUID orderId = UUID.randomUUID();

        when(paymentRepository
                .findByOrderId(orderId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getByOrderId(orderId)
        ).isInstanceOf(
                PaymentForOrderNotFoundException.class
        );
    }

    @Test
    void shouldRefundCompletedPayment() {
        Payment payment =
                payment(PaymentStatus.COMPLETED);

        when(paymentRepository
                .findByIdForUpdate(payment.getId()))
                .thenReturn(Optional.of(payment));

        when(paymentProcessor.refund(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency()
        )).thenReturn(
                PaymentProcessorResult.SUCCESS
        );

        when(paymentRepository.save(payment))
                .thenReturn(payment);

        Payment result =
                service.refund(payment.getId());

        assertThat(result.getStatus())
                .isEqualTo(PaymentStatus.REFUNDED);

        verify(paymentProcessor, times(1))
                .refund(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getAmount(),
                        payment.getCurrency()
                );
    }

    @Test
    void shouldReturnAlreadyRefundedPaymentWithoutRefundingAgain() {
        Payment payment =
                payment(PaymentStatus.REFUNDED);

        when(paymentRepository
                .findByIdForUpdate(payment.getId()))
                .thenReturn(Optional.of(payment));

        Payment result =
                service.refund(payment.getId());

        assertThat(result)
                .isSameAs(payment);

        verifyNoInteractions(paymentProcessor);

        verify(paymentRepository, never())
                .save(any());
    }

    @Test
    void shouldKeepCompletedStateWhenExternalRefundFails() {
        Payment payment =
                payment(PaymentStatus.COMPLETED);

        when(paymentRepository
                .findByIdForUpdate(payment.getId()))
                .thenReturn(Optional.of(payment));

        when(paymentProcessor.refund(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency()
        )).thenReturn(
                PaymentProcessorResult.FAILURE
        );

        assertThatThrownBy(() ->
                service.refund(payment.getId())
        ).isInstanceOf(
                PaymentRefundFailedException.class
        );

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.COMPLETED);

        verify(paymentRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectRefundForFailedPaymentBeforeCallingProcessor() {
        Payment payment =
                payment(PaymentStatus.FAILED);

        when(paymentRepository
                .findByIdForUpdate(payment.getId()))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() ->
                service.refund(payment.getId())
        ).isInstanceOf(
                InvalidPaymentStateException.class
        );

        verifyNoInteractions(paymentProcessor);
    }

    private Payment payment(
            PaymentStatus status
    ) {
        Instant now = Instant.now();

        return new Payment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("49.98"),
                "EUR",
                status,
                now,
                now
        );
    }

    private ProcessPaymentCommand commandFor(
            Payment payment
    ) {
        return new ProcessPaymentCommand(
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency()
        );
    }
}