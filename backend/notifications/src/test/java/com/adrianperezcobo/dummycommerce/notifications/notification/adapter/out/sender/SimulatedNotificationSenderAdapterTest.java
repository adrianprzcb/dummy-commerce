package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.out.sender;

import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.NotificationSenderResult;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationChannel;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulatedNotificationSenderAdapterTest {

    @Test
    void shouldReturnSuccess() {
        SimulatedNotificationSenderAdapter adapter =
                adapter("success");

        assertThat(send(adapter))
                .isEqualTo(
                        NotificationSenderResult.SUCCESS
                );
    }

    @Test
    void shouldReturnFailure() {
        SimulatedNotificationSenderAdapter adapter =
                adapter("failure");

        assertThat(send(adapter))
                .isEqualTo(
                        NotificationSenderResult.FAILURE
                );
    }

    @Test
    void shouldNormalizeConfiguredMode() {
        SimulatedNotificationSenderAdapter adapter =
                adapter("  SUCCESS ");

        assertThat(send(adapter))
                .isEqualTo(
                        NotificationSenderResult.SUCCESS
                );
    }

    @Test
    void shouldRejectUnknownMode() {
        SimulatedNotificationSenderAdapter adapter =
                adapter("random");

        assertThatThrownBy(() ->
                send(adapter)
        ).isInstanceOf(
                IllegalStateException.class
        );
    }

    @Test
    void shouldRejectNullMode() {
        SimulatedNotificationSenderAdapter adapter =
                adapter(null);

        assertThatThrownBy(() ->
                send(adapter)
        ).isInstanceOf(
                IllegalStateException.class
        );
    }

    private NotificationSenderResult send(
            SimulatedNotificationSenderAdapter adapter
    ) {
        return adapter.send(
                UUID.randomUUID(),
                NotificationChannel.EMAIL,
                "test@example.com",
                "Subject",
                "Message"
        );
    }

    private SimulatedNotificationSenderAdapter adapter(
            String mode
    ) {
        return new SimulatedNotificationSenderAdapter(
                new NotificationSimulatorProperties(
                        mode
                )
        );
    }
}