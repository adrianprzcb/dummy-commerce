package com.adrianperezcobo.dummycommerce.inventory.reservation.application.service;

import com.adrianperezcobo.dummycommerce.inventory.reservation.application.integration.inventory.*;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.out.StockReservationRepository;
import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.*;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.out.InventoryRepository;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;
import com.adrianperezcobo.dummycommerce.inventory.shared.inbox.InboxPort;
import com.adrianperezcobo.dummycommerce.inventory.shared.outbox.OutboxPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class InventorySagaHandler {
    private final InventoryRepository inventory;
    private final StockReservationRepository reservations;
    private final InboxPort inbox;
    private final OutboxPort outbox;

    public InventorySagaHandler(InventoryRepository inventory, StockReservationRepository reservations, InboxPort inbox, OutboxPort outbox) {
        this.inventory = inventory;
        this.reservations = reservations;
        this.inbox = inbox;
        this.outbox = outbox;
    }

    public void handle(ReserveStockCommandV1 command) {
        if (!inbox.claim(command.messageId(), InventoryTopics.RESERVE_STOCK_V1)) return;
        List<ReserveStockCommandV1.Item> requested = command.items().stream()
                .sorted(Comparator.comparing(item -> item.productId().toString())).toList();
        Map<UUID, InventoryItem> locked = new LinkedHashMap<>();
        String failure = null;
        for (var item : requested) {
            InventoryItem stock = inventory.findByProductIdForUpdate(item.productId()).orElse(null);
            if (stock == null) {
                failure = "INVENTORY_NOT_FOUND: " + item.productId();
            } else {
                locked.put(item.productId(), stock);
                if (stock.getAvailableQuantity() < item.quantity()) {
                    failure = "INSUFFICIENT_STOCK: " + item.productId() + ", requested=" + item.quantity() + ", available=" + stock.getAvailableQuantity();
                }
            }
        }
        List<StockReservation> existing = reservations.findByOrderId(command.orderId());
        if (!existing.isEmpty()) {
            // A different messageId for the same business command must not reserve twice.
            if (existing.size() != requested.size() || existing.stream().anyMatch(r -> r.getStatus() != ReservationStatus.RESERVED
                    || requested.stream().noneMatch(i -> i.productId().equals(r.getProductId()) && i.quantity() == r.getQuantity()))) {
                throw new IllegalStateException("Conflicting stock reservations for order " + command.orderId());
            }
            reserved(command.orderId());
            return;
        }
        if (failure != null) {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            publish(id, command.orderId(), InventoryTopics.STOCK_RESERVATION_FAILED_V1,
                    new StockReservationFailedEventV1(id, command.orderId(), now, failure), now);
            return;
        }
        // Validate the entire batch before making any change; persistence failures roll back all rows.
        for (var item : requested) {
            InventoryItem stock = locked.get(item.productId());
            stock.reserve(item.quantity());
            inventory.save(stock);
            reservations.save(new StockReservation(UUID.randomUUID(), command.orderId(), item.productId(), item.quantity(), ReservationStatus.RESERVED, Instant.now()));
        }
        reserved(command.orderId());
    }

    public void handle(ConfirmStockCommandV1 command) {
        if (!inbox.claim(command.messageId(), InventoryTopics.CONFIRM_STOCK_V1)) return;
        List<StockReservation> all = reservations.findByOrderIdForUpdate(command.orderId());
        if (all.isEmpty() || all.stream().anyMatch(r -> r.getStatus() == ReservationStatus.RELEASED)) {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            publish(id, command.orderId(), InventoryTopics.STOCK_CONFIRMATION_FAILED_V1,
                    new StockConfirmationFailedEventV1(id, command.orderId(), now,
                            all.isEmpty() ? "RESERVATIONS_NOT_FOUND" : "RESERVATIONS_RELEASED"), now);
            return;
        }
        Map<UUID, InventoryItem> locked = lockInventoryForReservations(all);
        for (StockReservation reservation : all) {
            if (reservation.getStatus() == ReservationStatus.CONFIRMED) continue;
            InventoryItem stock = locked.get(reservation.getProductId());
            stock.confirmReservation(reservation.getQuantity());
            reservation.confirm();
            inventory.save(stock);
            reservations.save(reservation);
        }
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        publish(id, command.orderId(), InventoryTopics.STOCK_CONFIRMED_V1, new StockConfirmedEventV1(id, command.orderId(), now), now);
    }

    public void handle(ReleaseStockCommandV1 command) {
        if (!inbox.claim(command.messageId(), InventoryTopics.RELEASE_STOCK_V1)) return;
        List<StockReservation> all = reservations.findByOrderIdForUpdate(command.orderId());
        Map<UUID, InventoryItem> locked = lockInventoryForReservations(all);

        if (all.stream().anyMatch(r -> r.getStatus() == ReservationStatus.CONFIRMED)) {
            throw new IllegalStateException("Cannot compensate confirmed stock");
        }
        for (StockReservation reservation : all) {
            if (reservation.getStatus() == ReservationStatus.RELEASED) continue;
            InventoryItem stock = locked.get(reservation.getProductId());
            stock.release(reservation.getQuantity());
            reservation.release();
            inventory.save(stock);
            reservations.save(reservation);
        }
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        publish(id, command.orderId(), InventoryTopics.STOCK_RELEASED_V1, new StockReleasedEventV1(id, command.orderId(), now), now);
    }

    private Map<UUID, InventoryItem> lockInventoryForReservations(List<StockReservation> all) {
        Map<UUID, InventoryItem> locked = new LinkedHashMap<>();
        all.stream().map(StockReservation::getProductId).distinct()
                .sorted(Comparator.comparing(UUID::toString)).forEach(id -> locked.put(id, inventory.findByProductIdForUpdate(id)
                        .orElseThrow(() -> new IllegalStateException("Missing inventory for reservation: " + id))));
        return locked;
    }

    private void reserved(UUID orderId) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        publish(id, orderId, InventoryTopics.STOCK_RESERVED_V1, new StockReservedEventV1(id, orderId, now), now);
    }

    private void publish(UUID id, UUID orderId, String topic, Object payload, Instant now) {
        outbox.save(id, orderId, topic, orderId.toString(), payload, now);
    }
}
