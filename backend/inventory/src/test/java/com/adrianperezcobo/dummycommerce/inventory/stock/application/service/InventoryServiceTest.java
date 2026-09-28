package com.adrianperezcobo.dummycommerce.inventory.stock.application.service;

import com.adrianperezcobo.dummycommerce.inventory.stock.application.command.ChangeStockCommand;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.command.CreateInventoryItemCommand;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.exception.InventoryItemAlreadyExistsException;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.exception.InventoryItemNotFoundException;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.out.InventoryRepository;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService service;

    @Test
    void shouldCreateInventoryItem() {
        UUID productId = UUID.randomUUID();

        when(inventoryRepository
                .existsByProductId(productId))
                .thenReturn(false);

        when(inventoryRepository.save(any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        InventoryItem result = service.create(
                new CreateInventoryItemCommand(
                        productId,
                        10
                )
        );

        assertThat(result.getProductId())
                .isEqualTo(productId);

        assertThat(result.getAvailableQuantity())
                .isEqualTo(10);

        assertThat(result.getReservedQuantity())
                .isZero();

        verify(inventoryRepository)
                .save(any(InventoryItem.class));
    }

    @Test
    void shouldRejectDuplicatedInventoryItem() {
        UUID productId = UUID.randomUUID();

        when(inventoryRepository
                .existsByProductId(productId))
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.create(
                        new CreateInventoryItemCommand(
                                productId,
                                10
                        )
                )
        ).isInstanceOf(
                InventoryItemAlreadyExistsException.class
        );

        verify(inventoryRepository, never())
                .save(any());
    }

    @Test
    void shouldGetInventoryItem() {
        InventoryItem item =
                new InventoryItem(
                        UUID.randomUUID(),
                        10,
                        2
                );

        when(inventoryRepository.findByProductId(
                item.getProductId()
        )).thenReturn(Optional.of(item));

        InventoryItem result =
                service.getByProductId(
                        item.getProductId()
                );

        assertThat(result).isSameAs(item);
    }

    @Test
    void shouldThrowWhenInventoryDoesNotExist() {
        UUID productId = UUID.randomUUID();

        when(inventoryRepository
                .findByProductId(productId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getByProductId(productId)
        ).isInstanceOf(
                InventoryItemNotFoundException.class
        );
    }

    @Test
    void shouldIncreaseStockUsingLockedRead() {
        InventoryItem item =
                new InventoryItem(
                        UUID.randomUUID(),
                        10,
                        0
                );

        when(inventoryRepository
                .findByProductIdForUpdate(
                        item.getProductId()
                ))
                .thenReturn(Optional.of(item));

        when(inventoryRepository.save(item))
                .thenReturn(item);

        InventoryItem result = service.increase(
                new ChangeStockCommand(
                        item.getProductId(),
                        5
                )
        );

        assertThat(result.getAvailableQuantity())
                .isEqualTo(15);

        verify(inventoryRepository)
                .findByProductIdForUpdate(
                        item.getProductId()
                );
    }

    @Test
    void shouldDecreaseStockUsingLockedRead() {
        InventoryItem item =
                new InventoryItem(
                        UUID.randomUUID(),
                        10,
                        0
                );

        when(inventoryRepository
                .findByProductIdForUpdate(
                        item.getProductId()
                ))
                .thenReturn(Optional.of(item));

        when(inventoryRepository.save(item))
                .thenReturn(item);

        InventoryItem result = service.decrease(
                new ChangeStockCommand(
                        item.getProductId(),
                        4
                )
        );

        assertThat(result.getAvailableQuantity())
                .isEqualTo(6);
    }

    @Test
    void shouldRejectDecreaseWithInsufficientStock() {
        InventoryItem item =
                new InventoryItem(
                        UUID.randomUUID(),
                        2,
                        0
                );

        when(inventoryRepository
                .findByProductIdForUpdate(
                        item.getProductId()
                ))
                .thenReturn(Optional.of(item));

        assertThatThrownBy(() ->
                service.decrease(
                        new ChangeStockCommand(
                                item.getProductId(),
                                3
                        )
                )
        ).isInstanceOf(
                InsufficientStockException.class
        );

        verify(inventoryRepository, never())
                .save(any());
    }
}