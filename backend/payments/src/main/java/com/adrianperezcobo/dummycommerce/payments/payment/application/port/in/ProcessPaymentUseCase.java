package com.adrianperezcobo.dummycommerce.payments.payment.application.port.in;

import com.adrianperezcobo.dummycommerce.payments.payment.application.command.ProcessPaymentCommand;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.Payment;

public interface ProcessPaymentUseCase {

    Payment process(
            ProcessPaymentCommand command
    );
}