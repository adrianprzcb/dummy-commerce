package com.adrianperezcobo.dummycommerce.payments.payment.application.service;

import com.adrianperezcobo.dummycommerce.payments.payment.application.command.ProcessPaymentCommand;
import com.adrianperezcobo.dummycommerce.payments.payment.application.exception.PaymentForOrderNotFoundException;
import com.adrianperezcobo.dummycommerce.payments.payment.application.exception.PaymentNotFoundException;
import com.adrianperezcobo.dummycommerce.payments.payment.application.exception.PaymentRefundFailedException;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.in.GetPaymentByOrderUseCase;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.in.GetPaymentUseCase;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.in.ProcessPaymentUseCase;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.in.RefundPaymentUseCase;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.out.PaymentProcessorPort;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.out.PaymentProcessorResult;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.out.PaymentRepository;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.Payment;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.PaymentStatus;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.exception.InvalidPaymentStateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService implements
        ProcessPaymentUseCase,
        GetPaymentUseCase,
        GetPaymentByOrderUseCase,
        RefundPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentProcessorPort paymentProcessor;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentProcessorPort paymentProcessor
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentProcessor = paymentProcessor;
    }

    @Override
    @Transactional
    public Payment process(
            ProcessPaymentCommand command
    ) {
        /*
         * Idempotencia básica:
         *
         * Si ese pedido ya tiene un pago finalizado,
         * fallido o reembolsado, devolvemos exactamente
         * ese pago y no procesamos un segundo cobro.
         */
        Payment existing =
                paymentRepository
                        .findByOrderIdForUpdate(
                                command.orderId()
                        )
                        .orElse(null);

        if (existing != null) {
            if (existing.getStatus()
                    != PaymentStatus.PENDING) {

                return existing;
            }

            return processPending(existing);
        }

        Instant now = Instant.now();

        Payment payment = new Payment(
                UUID.randomUUID(),
                command.orderId(),
                command.amount(),
                command.currency(),
                PaymentStatus.PENDING,
                now,
                now
        );

        payment = paymentRepository.save(
                payment
        );

        return processPending(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getById(
            UUID paymentId
    ) {
        return paymentRepository
                .findById(paymentId)
                .orElseThrow(
                        () ->
                                new PaymentNotFoundException(
                                        paymentId
                                )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getByOrderId(
            UUID orderId
    ) {
        return paymentRepository
                .findByOrderId(orderId)
                .orElseThrow(
                        () ->
                                new PaymentForOrderNotFoundException(
                                        orderId
                                )
                );
    }

    @Override
    @Transactional
    public Payment refund(
            UUID paymentId
    ) {
        Payment payment =
                paymentRepository
                        .findByIdForUpdate(
                                paymentId
                        )
                        .orElseThrow(
                                () ->
                                        new PaymentNotFoundException(
                                                paymentId
                                        )
                        );

        /*
         * Refund idempotente:
         * repetir el mismo comando no devuelve
         * el dinero dos veces.
         */
        if (payment.getStatus()
                == PaymentStatus.REFUNDED) {

            return payment;
        }

        /*
         * Esta comprobación se hace ANTES de llamar
         * al proveedor externo.
         */
        if (payment.getStatus()
                != PaymentStatus.COMPLETED) {

            throw new InvalidPaymentStateException(
                    "Payment must be COMPLETED but is " +
                            payment.getStatus()
            );
        }

        PaymentProcessorResult result =
                paymentProcessor.refund(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getAmount(),
                        payment.getCurrency()
                );

        if (result
                != PaymentProcessorResult.SUCCESS) {

            throw new PaymentRefundFailedException(
                    payment.getId()
            );
        }

        payment.refund(
                Instant.now()
        );

        return paymentRepository.save(
                payment
        );
    }

    private Payment processPending(
            Payment payment
    ) {
        PaymentProcessorResult result =
                paymentProcessor.process(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getAmount(),
                        payment.getCurrency()
                );

        Instant now = Instant.now();

        if (result
                == PaymentProcessorResult.SUCCESS) {

            payment.complete(now);

        } else {
            payment.fail(now);
        }

        return paymentRepository.save(
                payment
        );
    }
}