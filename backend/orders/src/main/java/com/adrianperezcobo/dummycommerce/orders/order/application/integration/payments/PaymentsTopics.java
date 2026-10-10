package com.adrianperezcobo.dummycommerce.orders.order.application.integration.payments;

public final class PaymentsTopics {
    public static final String PROCESS_PAYMENT_V1 = "dummy-commerce.payments.process-payment.v1";
    public static final String PAYMENT_COMPLETED_V1 = "dummy-commerce.payments.payment-completed.v1";
    public static final String PAYMENT_FAILED_V1 = "dummy-commerce.payments.payment-failed.v1";

    public static final String REFUND_PAYMENT_V1 = "dummy-commerce.payments.refund-payment.v1";
    public static final String PAYMENT_REFUNDED_V1 = "dummy-commerce.payments.payment-refunded.v1";
    public static final String PAYMENT_REFUND_FAILED_V1 = "dummy-commerce.payments.payment-refund-failed.v1";

    private PaymentsTopics() { }
}
