package com.adrianperezcobo.dummycommerce.payments.shared.inbox;

import java.util.UUID;

public interface InboxPort {
    // Claim and business changes must commit or roll back in the same transaction.
    boolean claim(UUID messageId, String topic);
}
