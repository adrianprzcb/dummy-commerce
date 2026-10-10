package com.adrianperezcobo.dummycommerce.payments.payment.application.service;

import com.adrianperezcobo.dummycommerce.payments.payment.application.command.ProcessPaymentCommand;
import com.adrianperezcobo.dummycommerce.payments.payment.application.integration.payments.*;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.*;
import com.adrianperezcobo.dummycommerce.payments.payment.application.exception.PaymentRefundFailedException;
import com.adrianperezcobo.dummycommerce.payments.shared.inbox.InboxPort;
import com.adrianperezcobo.dummycommerce.payments.shared.outbox.OutboxPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentSagaHandler {
    private final PaymentService payments;
    private final InboxPort inbox;
    private final OutboxPort outbox;

    public PaymentSagaHandler(PaymentService payments, InboxPort inbox, OutboxPort outbox) {
        this.payments = payments;
        this.inbox = inbox;
        this.outbox = outbox;
    }

    @Transactional
    public void handle(ProcessPaymentCommandV1 command) {
        if (!inbox.claim(command.messageId(), PaymentsTopics.PROCESS_PAYMENT_V1)) return;
        Payment payment = payments.process(new ProcessPaymentCommand(command.orderId(), command.amount(), command.currency()));
        if (payment.getAmount().compareTo(command.amount()) != 0 || !payment.getCurrency().equals(command.currency())) {
            throw new IllegalArgumentException("Payment command conflicts with existing payment");
        }
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String topic;
        Object event;
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            topic = PaymentsTopics.PAYMENT_COMPLETED_V1;
            event = new PaymentCompletedEventV1(id, payment.getOrderId(), payment.getId(), now, payment.getAmount(), payment.getCurrency());
        } else if (payment.getStatus() == PaymentStatus.FAILED) {
            topic = PaymentsTopics.PAYMENT_FAILED_V1;
            event = new PaymentFailedEventV1(id, payment.getOrderId(), payment.getId(), now, payment.getAmount(), payment.getCurrency(), "PAYMENT_PROCESSOR_REJECTED");
        } else {
            throw new IllegalStateException("Unexpected Saga payment status: " + payment.getStatus());
        }
        outbox.save(id, command.orderId(), topic, command.orderId().toString(), event, now);
    }
    @Transactional
    public void handle(RefundPaymentCommandV1 command) {
        if (!inbox.claim(command.messageId(), PaymentsTopics.REFUND_PAYMENT_V1)) return;
        Payment payment = payments.getByOrderId(command.orderId());
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String topic;
        Object event;
        try {
            payment = payments.refund(payment.getId());
            topic = PaymentsTopics.PAYMENT_REFUNDED_V1;
            event = new PaymentRefundedEventV1(id, payment.getOrderId(), payment.getId(), now,
                    payment.getAmount(), payment.getCurrency());
        } catch (PaymentRefundFailedException ex) {
            topic = PaymentsTopics.PAYMENT_REFUND_FAILED_V1;
            event = new PaymentRefundFailedEventV1(id, payment.getOrderId(), payment.getId(), now,
                    payment.getAmount(), payment.getCurrency(), "PAYMENT_REFUND_REJECTED");
        }
        outbox.save(id, command.orderId(), topic, command.orderId().toString(), event, now);
    }
}

