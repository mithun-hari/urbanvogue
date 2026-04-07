package com.urbanvogue.inventory_service.service;

import com.urbanvogue.inventory_service.model.Inventory;
import com.urbanvogue.inventory_service.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProductId(10L);
        inventory.setAvailableQuantity(50);
        inventory.setReservedQuantity(5);
    }

    @Test
    @DisplayName("getInventoryByProductId - Returns inventory when found")
    void getInventoryByProductId_Success() {
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

        Inventory result = inventoryService.getInventoryByProductId(10L);

        assertNotNull(result);
        assertEquals(10L, result.getProductId());
        assertEquals(50, result.getAvailableQuantity());
    }

    @Test
    @DisplayName("getInventoryByProductId - Not found throws RuntimeException")
    void getInventoryByProductId_NotFound_Throws() {
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                inventoryService.getInventoryByProductId(999L));

        assertEquals("Inventory not found for product", ex.getMessage());
    }

    @Test
    @DisplayName("deductInventory - Reduces available quantity correctly")
    void deductInventory_Success() {
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

        inventoryService.deductInventory(10L, 15);

        assertEquals(35, inventory.getAvailableQuantity()); // 50 - 15
        verify(inventoryRepository).save(inventory);
    }

    @Test
    @DisplayName("deductInventory - Not enough stock throws RuntimeException")
    void deductInventory_InsufficientStock_Throws() {
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                inventoryService.deductInventory(10L, 100));

        assertEquals("Not enough stock", ex.getMessage());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("restoreInventory - Adds quantity to existing inventory")
    void restoreInventory_ExistingProduct_AddsQuantity() {
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

        inventoryService.restoreInventory(10L, 20);

        assertEquals(70, inventory.getAvailableQuantity()); // 50 + 20
        verify(inventoryRepository).save(inventory);
    }

    @Test
    @DisplayName("restoreInventory - Creates new record for unknown product")
    void restoreInventory_NewProduct_CreatesRecord() {
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        inventoryService.restoreInventory(99L, 25);

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());

        Inventory saved = captor.getValue();
        assertEquals(99L, saved.getProductId());
        assertEquals(25, saved.getAvailableQuantity()); // 0 + 25
        assertEquals(0, saved.getReservedQuantity());
    }
}
