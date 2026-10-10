package com.adrianperezcobo.dummycommerce.payments.payment.application.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.adrianperezcobo.dummycommerce.payments.payment.application.integration.payments.*;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.out.*;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.PaymentStatus;
import com.adrianperezcobo.dummycommerce.payments.shared.outbox.OutboxPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class PaymentSagaIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @MockitoSpyBean OutboxPort outbox;

    @Autowired PaymentSagaHandler handler;
    @Autowired PaymentService payments;
    @MockitoBean PaymentProcessorPort processor;

    @Test
    void completesAndWritesPaymentCompletedWithBusinessIdempotency() {
        ProcessPaymentCommandV1 command = command();
        doReturn(PaymentProcessorResult.SUCCESS).when(processor).process(any(), any(), any(), anyString());
        handler.handle(command);
        handler.handle(command);
        PaymentCompletedEventV1 result = event(command.orderId(), PaymentsTopics.PAYMENT_COMPLETED_V1, PaymentCompletedEventV1.class);
        assertThat(result.paymentId()).isEqualTo(payments.getByOrderId(command.orderId()).getId());
        assertThat(result.amount()).isEqualByComparingTo(command.amount());
        assertThat(result.currency()).isEqualTo(command.currency());
        assertThat(count("outbox_messages", "aggregate_id", command.orderId())).isEqualTo(1);
        handler.handle(new ProcessPaymentCommandV1(UUID.randomUUID(), command.orderId(), Instant.now(), command.amount(), command.currency()));
        verify(processor, times(1)).process(any(), any(), any(), anyString());
        assertThat(count("payments", "order_id", command.orderId())).isEqualTo(1);
    }

    @Test
    void rejectedPaymentWritesPaymentFailedAndSkipsDuplicate() {
        ProcessPaymentCommandV1 command = command();
        when(processor.process(any(), any(), any(), anyString())).thenReturn(PaymentProcessorResult.FAILURE);
        handler.handle(command);
        handler.handle(command);
        assertThat(payments.getByOrderId(command.orderId()).getStatus()).isEqualTo(PaymentStatus.FAILED);
        PaymentFailedEventV1 result = event(command.orderId(), PaymentsTopics.PAYMENT_FAILED_V1, PaymentFailedEventV1.class);
        assertThat(result.reason()).isEqualTo("PAYMENT_PROCESSOR_REJECTED");
        assertThat(result.paymentId()).isEqualTo(payments.getByOrderId(command.orderId()).getId());
        verify(processor, times(1)).process(any(), any(), any(), anyString());
    }

    @Test
    void technicalProcessorFailureRollsBackPaymentInboxAndOutbox() {
        ProcessPaymentCommandV1 command = command();
        when(processor.process(any(), any(), any(), anyString())).thenThrow(new IllegalStateException("technical failure"));
        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(IllegalStateException.class);
        assertThat(count("payments", "order_id", command.orderId())).isZero();
        assertThat(count("inbox_messages", "message_id", command.messageId())).isZero();
        assertThat(count("outbox_messages", "aggregate_id", command.orderId())).isZero();
        doReturn(PaymentProcessorResult.SUCCESS).when(processor).process(any(), any(), any(), anyString());
        handler.handle(command);
        assertThat(payments.getByOrderId(command.orderId()).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void outboxFailureRollsBackPaymentAndInbox() {
        ProcessPaymentCommandV1 command = command();
        doReturn(PaymentProcessorResult.SUCCESS).when(processor).process(any(), any(), any(), anyString());
        doThrow(new IllegalStateException("outbox unavailable")).when(outboxSpy()).save(any(), any(), anyString(), anyString(), any(), any());
        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(IllegalStateException.class);
        assertThat(count("payments", "order_id", command.orderId())).isZero();
        assertThat(count("inbox_messages", "message_id", command.messageId())).isZero();
    }

    @Test
    void refundIsIdempotentAfterCommitBeforeAcknowledgementAndAcrossBusinessRedelivery() {
        ProcessPaymentCommandV1 process = command();
        when(processor.process(any(), any(), any(), anyString())).thenReturn(PaymentProcessorResult.SUCCESS);
        when(processor.refund(any(), any(), any(), anyString())).thenReturn(PaymentProcessorResult.SUCCESS);
        handler.handle(process);
        var command = new RefundPaymentCommandV1(UUID.randomUUID(), process.orderId(), Instant.now());
        handler.handle(command);
        handler.handle(command);
        handler.handle(new RefundPaymentCommandV1(UUID.randomUUID(), process.orderId(), Instant.now()));
        assertThat(payments.getByOrderId(process.orderId()).getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(event(process.orderId(), PaymentsTopics.PAYMENT_REFUNDED_V1, PaymentRefundedEventV1.class).paymentId())
                .isEqualTo(payments.getByOrderId(process.orderId()).getId());
        verify(processor, times(1)).refund(any(), any(), any(), anyString());
    }

    @Test
    void refundRejectionCommitsFailureEventAndCanBeRetriedWithNewCommand() {
        ProcessPaymentCommandV1 process = command();
        when(processor.process(any(), any(), any(), anyString())).thenReturn(PaymentProcessorResult.SUCCESS);
        when(processor.refund(any(), any(), any(), anyString())).thenReturn(PaymentProcessorResult.FAILURE);
        handler.handle(process);
        var rejected = new RefundPaymentCommandV1(UUID.randomUUID(), process.orderId(), Instant.now());
        handler.handle(rejected);
        handler.handle(rejected);
        assertThat(payments.getByOrderId(process.orderId()).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        event(process.orderId(), PaymentsTopics.PAYMENT_REFUND_FAILED_V1, PaymentRefundFailedEventV1.class);
        assertThat(count("inbox_messages", "message_id", rejected.messageId())).isEqualTo(1);
        verify(processor, times(1)).refund(any(), any(), any(), anyString());
        when(processor.refund(any(), any(), any(), anyString())).thenReturn(PaymentProcessorResult.SUCCESS);
        handler.handle(new RefundPaymentCommandV1(UUID.randomUUID(), process.orderId(), Instant.now()));
        assertThat(payments.getByOrderId(process.orderId()).getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void technicalRefundFailureRollsBackInboxAndLeavesPaymentCompleted() {
        ProcessPaymentCommandV1 process = command();
        when(processor.process(any(), any(), any(), anyString())).thenReturn(PaymentProcessorResult.SUCCESS);
        handler.handle(process);
        when(processor.refund(any(), any(), any(), anyString())).thenThrow(new IllegalStateException("provider down"));
        var command = new RefundPaymentCommandV1(UUID.randomUUID(), process.orderId(), Instant.now());
        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(IllegalStateException.class);
        assertThat(count("inbox_messages", "message_id", command.messageId())).isZero();
        assertThat(payments.getByOrderId(process.orderId()).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void refundOutboxFailureRollsBackRefundAndInbox() {
        ProcessPaymentCommandV1 process = command();
        when(processor.process(any(), any(), any(), anyString())).thenReturn(PaymentProcessorResult.SUCCESS);
        when(processor.refund(any(), any(), any(), anyString())).thenReturn(PaymentProcessorResult.SUCCESS);
        handler.handle(process);
        doThrow(new IllegalStateException("outbox unavailable")).when(outboxSpy()).save(any(), any(), anyString(), anyString(), any(), any());
        var command = new RefundPaymentCommandV1(UUID.randomUUID(), process.orderId(), Instant.now());
        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(IllegalStateException.class);
        assertThat(payments.getByOrderId(process.orderId()).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(count("inbox_messages", "message_id", command.messageId())).isZero();
    }

    private ProcessPaymentCommandV1 command() {
        return new ProcessPaymentCommandV1(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), new BigDecimal("24.68"), "EUR");
    }

    private OutboxPort outboxSpy() {
        return AopTestUtils.getUltimateTargetObject(outbox);
    }

    private long count(String table, String column, UUID id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Long.class, id);
    }

    private <T> T event(UUID orderId, String topic, Class<T> type) {
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM outbox_messages WHERE aggregate_id = ? AND topic = ? ORDER BY created_at DESC LIMIT 1", orderId, topic);
        assertThat(row.get("message_key")).isEqualTo(orderId.toString());
        assertThat(row.get("status")).isEqualTo("PENDING");
        String payload = (String) row.get("payload");
        var tree = json.readTree(payload);
        assertThat(tree.get("messageId").asText()).isEqualTo(row.get("id").toString());
        assertThat(tree.get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(tree.get("occurredAt").asText()).isNotBlank();
        return json.readValue(payload, type);
    }
}
