package com.urbanvogue.inventory_service.service;

import com.urbanvogue.inventory_service.model.Inventory;
import com.urbanvogue.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public Inventory getInventoryByProductId(Long productId) {

        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product"));
    }
    public void deductInventory(Long productId, Integer quantity) {

        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Product inventory not found"));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new RuntimeException("Not enough stock");
        }

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() - quantity
        );

        inventoryRepository.save(inventory);
    }
    public void restoreInventory(Long productId, Integer quantity) {

        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() + quantity
        );

        inventoryRepository.save(inventory);
    }
}