package com.urbanvogue.order_service.client;

import com.urbanvogue.order_service.dto.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "inventory-service", url = "http://localhost:8086")
public interface InventoryClient {

    @GetMapping("/api/inventory/{productId}")
    InventoryResponse getInventory(@PathVariable("productId") Long productId);

    @PostMapping("/api/inventory/deduct")
    void deductInventory(@RequestParam("productId") Long productId,
                         @RequestParam("quantity") Integer quantity);
    @PostMapping("/api/inventory/restore")
    void restoreInventory(@RequestParam("productId") Long productId,
                          @RequestParam("quantity") Integer quantity);
}