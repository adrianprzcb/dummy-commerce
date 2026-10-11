package com.adrianperezcobo.dummycommerce.inventory.stock.application.service;

import com.adrianperezcobo.dummycommerce.inventory.stock.application.command.ChangeStockCommand;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.command.CreateInventoryItemCommand;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.exception.InventoryItemAlreadyExistsException;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.exception.InventoryItemNotFoundException;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in.CreateInventoryItemUseCase;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in.DecreaseStockUseCase;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in.GetInventoryUseCase;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in.GetStockAvailabilityUseCase;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.result.StockAvailability;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in.IncreaseStockUseCase;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.out.InventoryRepository;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService implements
        CreateInventoryItemUseCase,
        GetInventoryUseCase,
        GetStockAvailabilityUseCase,
        IncreaseStockUseCase,
        DecreaseStockUseCase {

    private final InventoryRepository inventoryRepository;

    public InventoryService(
            InventoryRepository inventoryRepository
    ) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional
    public InventoryItem create(
            CreateInventoryItemCommand command
    ) {
        if (inventoryRepository.existsByProductId(
                command.productId()
        )) {
            throw new InventoryItemAlreadyExistsException(
                    command.productId()
            );
        }

        InventoryItem item = new InventoryItem(
                command.productId(),
                command.initialQuantity(),
                0
        );

        return inventoryRepository.save(item);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItem getByProductId(
            UUID productId
    ) {
        return inventoryRepository
                .findByProductId(productId)
                .orElseThrow(
                        () -> new InventoryItemNotFoundException(
                                productId
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAvailability> getAvailability(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty() || productIds.size() > 100
                || productIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Between 1 and 100 product identifiers are required");
        }
        var uniqueIds = productIds.stream().distinct().toList();
        var quantities = inventoryRepository.findAllByProductIds(uniqueIds).stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, InventoryItem::getAvailableQuantity));
        return uniqueIds.stream()
                .map(id -> new StockAvailability(id, quantities.getOrDefault(id, 0)))
                .toList();
    }

    @Override
    @Transactional
    public InventoryItem increase(
            ChangeStockCommand command
    ) {
        InventoryItem item = inventoryRepository
                .findByProductIdForUpdate(
                        command.productId()
                )
                .orElseThrow(
                        () -> new InventoryItemNotFoundException(
                                command.productId()
                        )
                );

        item.increaseStock(command.quantity());

        return inventoryRepository.save(item);
    }

    @Override
    @Transactional
    public InventoryItem decrease(
            ChangeStockCommand command
    ) {
        InventoryItem item = inventoryRepository
                .findByProductIdForUpdate(
                        command.productId()
                )
                .orElseThrow(
                        () -> new InventoryItemNotFoundException(
                                command.productId()
                        )
                );

        item.decreaseStock(command.quantity());

        return inventoryRepository.save(item);
    }
}
