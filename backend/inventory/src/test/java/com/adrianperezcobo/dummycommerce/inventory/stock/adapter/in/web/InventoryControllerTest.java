package com.adrianperezcobo.dummycommerce.inventory.stock.adapter.in.web;

import com.adrianperezcobo.dummycommerce.inventory.shared.adapter.in.web.GlobalExceptionHandler;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.command.ChangeStockCommand;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.command.CreateInventoryItemCommand;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.exception.InventoryItemAlreadyExistsException;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.exception.InventoryItemNotFoundException;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in.CreateInventoryItemUseCase;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in.DecreaseStockUseCase;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in.GetInventoryUseCase;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in.IncreaseStockUseCase;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@Import({
        InventoryWebMapper.class,
        GlobalExceptionHandler.class
})
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateInventoryItemUseCase createInventoryItemUseCase;

    @MockitoBean
    private GetInventoryUseCase getInventoryUseCase;

    @MockitoBean
    private IncreaseStockUseCase increaseStockUseCase;

    @MockitoBean
    private DecreaseStockUseCase decreaseStockUseCase;

    @Test
    void shouldCreateInventoryItem() throws Exception {
        UUID productId = UUID.randomUUID();

        InventoryItem item =
                new InventoryItem(
                        productId,
                        10,
                        0
                );

        when(createInventoryItemUseCase.create(
                any(CreateInventoryItemCommand.class)
        )).thenReturn(item);

        mockMvc.perform(
                        post("/api/inventory")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "productId": "%s",
                                          "initialQuantity": 10
                                        }
                                        """.formatted(productId))
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.productId")
                                .value(productId.toString())
                )
                .andExpect(
                        jsonPath("$.availableQuantity")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.reservedQuantity")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.totalQuantity")
                                .value(10)
                );
    }

    @Test
    void shouldRejectNegativeInitialQuantity()
            throws Exception {

        mockMvc.perform(
                        post("/api/inventory")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "productId": "%s",
                                          "initialQuantity": -1
                                        }
                                        """.formatted(
                                        UUID.randomUUID()
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.title")
                                .value("Validation error")
                )
                .andExpect(
                        jsonPath(
                                "$.errors.initialQuantity"
                        ).exists()
                );
    }

    @Test
    void shouldReturnConflictWhenInventoryAlreadyExists()
            throws Exception {

        UUID productId = UUID.randomUUID();

        when(createInventoryItemUseCase.create(
                any(CreateInventoryItemCommand.class)
        )).thenThrow(
                new InventoryItemAlreadyExistsException(
                        productId
                )
        );

        mockMvc.perform(
                        post("/api/inventory")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "productId": "%s",
                                          "initialQuantity": 10
                                        }
                                        """.formatted(productId))
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Inventory item already exists"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                );
    }

    @Test
    void shouldGetInventoryItem() throws Exception {
        UUID productId = UUID.randomUUID();

        when(getInventoryUseCase
                .getByProductId(productId))
                .thenReturn(
                        new InventoryItem(
                                productId,
                                7,
                                3
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/inventory/{productId}",
                                productId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.availableQuantity")
                                .value(7)
                )
                .andExpect(
                        jsonPath("$.reservedQuantity")
                                .value(3)
                )
                .andExpect(
                        jsonPath("$.totalQuantity")
                                .value(10)
                );
    }

    @Test
    void shouldReturnNotFoundWhenInventoryDoesNotExist()
            throws Exception {

        UUID productId = UUID.randomUUID();

        when(getInventoryUseCase
                .getByProductId(productId))
                .thenThrow(
                        new InventoryItemNotFoundException(
                                productId
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/inventory/{productId}",
                                productId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Inventory item not found"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                );
    }

    @Test
    void shouldIncreaseStock() throws Exception {
        UUID productId = UUID.randomUUID();

        when(increaseStockUseCase.increase(
                any(ChangeStockCommand.class)
        )).thenReturn(
                new InventoryItem(
                        productId,
                        15,
                        0
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/inventory/{productId}/increase",
                                productId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "quantity": 5
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.availableQuantity")
                                .value(15)
                );
    }

    @Test
    void shouldReturnConflictForInsufficientStock()
            throws Exception {

        UUID productId = UUID.randomUUID();

        when(decreaseStockUseCase.decrease(
                any(ChangeStockCommand.class)
        )).thenThrow(
                new InsufficientStockException(
                        productId,
                        5,
                        2
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/inventory/{productId}/decrease",
                                productId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "quantity": 5
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value("Insufficient stock")
                )
                .andExpect(
                        jsonPath("$.productId")
                                .value(productId.toString())
                )
                .andExpect(
                        jsonPath("$.requested")
                                .value(5)
                )
                .andExpect(
                        jsonPath("$.available")
                                .value(2)
                );
    }

    @Test
    void shouldRejectNonPositiveStockChange()
            throws Exception {

        mockMvc.perform(
                        patch(
                                "/api/inventory/{productId}/increase",
                                UUID.randomUUID()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "quantity": 0
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.errors.quantity")
                                .exists()
                );
    }
}