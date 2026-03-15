package com.urbanvogue.inventory_service.controller;

import com.urbanvogue.inventory_service.model.Inventory;
import com.urbanvogue.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public Inventory getInventory(@PathVariable Long productId) {
        return inventoryService.getInventoryByProductId(productId);
    }

    @PostMapping("/deduct")
    public void deductInventory(@RequestParam Long productId,
                                @RequestParam Integer quantity) {
        inventoryService.deductInventory(productId, quantity);
    }
    @PostMapping("/restore")
    public void restoreInventory(@RequestParam Long productId,
                                 @RequestParam Integer quantity) {
        inventoryService.restoreInventory(productId, quantity);
    }
}