package com.urbanvogue.inventory_service.integration;

import com.urbanvogue.inventory_service.model.Inventory;
import com.urbanvogue.inventory_service.repository.InventoryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("InventoryService - Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void cleanDatabase() {
        inventoryRepository.deleteAll();
    }

    private Inventory seedInventory(Long productId, int available, int reserved) {
        Inventory inv = new Inventory();
        inv.setProductId(productId);
        inv.setAvailableQuantity(available);
        inv.setReservedQuantity(reserved);
        return inventoryRepository.save(inv);
    }

    @Test
    @Order(1)
    @DisplayName("INT-01: GET /api/inventory/{productId} - Fetch inventory from real DB")
    @WithMockUser
    void getInventory_ShouldReturnFromDatabase() throws Exception {
        seedInventory(100L, 500, 10);

        mockMvc.perform(get("/api/inventory/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(100))
                .andExpect(jsonPath("$.availableQuantity").value(500))
                .andExpect(jsonPath("$.reservedQuantity").value(10));

        System.out.println("  [PASS] INT-01: Inventory fetched from DB for productId=100");
    }

    @Test
    @Order(2)
    @DisplayName("INT-02: POST /api/inventory/deduct - Deduct and verify DB state changes")
    @WithMockUser
    void deductInventory_ShouldUpdateDatabase() throws Exception {
        seedInventory(200L, 100, 0);

        mockMvc.perform(post("/api/inventory/deduct")
                        .param("productId", "200")
                        .param("quantity", "25"))
                .andExpect(status().isOk());

        // Verify DB was updated
        Inventory updated = inventoryRepository.findByProductId(200L).orElseThrow();
        assertEquals(75, updated.getAvailableQuantity(), "Available should be 100-25=75");
        System.out.println("  [PASS] INT-02: Inventory deducted, available=75 (was 100)");
    }

    @Test
    @Order(3)
    @DisplayName("INT-03: POST /api/inventory/restore - Restore and verify DB state")
    @WithMockUser
    void restoreInventory_ShouldUpdateDatabase() throws Exception {
        seedInventory(300L, 50, 20);

        mockMvc.perform(post("/api/inventory/restore")
                        .param("productId", "300")
                        .param("quantity", "20"))
                .andExpect(status().isOk());

        Inventory updated = inventoryRepository.findByProductId(300L).orElseThrow();
        assertEquals(70, updated.getAvailableQuantity(), "Available should be 50+20=70");
        System.out.println("  [PASS] INT-03: Inventory restored, available=70 (was 50)");
    }

    @Test
    @Order(4)
    @DisplayName("INT-04: POST /api/inventory/add-stock - Admin stock addition with role header")
    @WithMockUser
    void addStock_WithAdminRole_ShouldUpdateDatabase() throws Exception {
        seedInventory(400L, 200, 0);

        mockMvc.perform(post("/api/inventory/add-stock")
                        .header("X-User-Role", "ADMIN")
                        .param("productId", "400")
                        .param("quantity", "100"))
                .andExpect(status().isOk());

        Inventory updated = inventoryRepository.findByProductId(400L).orElseThrow();
        assertEquals(300, updated.getAvailableQuantity(), "Available should be 200+100=300");
        System.out.println("  [PASS] INT-04: Admin stock added, available=300 (was 200)");
    }

    @Test
    @Order(5)
    @DisplayName("INT-05: POST /api/inventory/add-stock - Non-admin should be rejected")
    @WithMockUser
    void addStock_WithoutAdminRole_ShouldFail() throws Exception {
        seedInventory(500L, 100, 0);

        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/api/inventory/add-stock")
                            .header("X-User-Role", "USER")
                            .param("productId", "500")
                            .param("quantity", "50"));
        });
        assertTrue(exception.getCause().getMessage().contains("UNAUTHORIZED"));

        // Verify DB was NOT changed
        Inventory unchanged = inventoryRepository.findByProductId(500L).orElseThrow();
        assertEquals(100, unchanged.getAvailableQuantity(), "Stock should remain unchanged");
        System.out.println("  [PASS] INT-05: Non-admin rejected, stock unchanged at 100");
    }

    @Test
    @Order(6)
    @DisplayName("INT-06: POST /api/inventory/deduct - Insufficient stock should fail")
    @WithMockUser
    void deductInventory_InsufficientStock_ShouldFail() throws Exception {
        seedInventory(600L, 5, 0);

        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/api/inventory/deduct")
                            .param("productId", "600")
                            .param("quantity", "100"));
        });
        assertTrue(exception.getCause().getMessage().contains("Not enough stock"));

        // Verify DB was NOT changed
        Inventory unchanged = inventoryRepository.findByProductId(600L).orElseThrow();
        assertEquals(5, unchanged.getAvailableQuantity(), "Stock should remain at 5");
        System.out.println("  [PASS] INT-06: Insufficient stock deduction rejected, stock=5");
    }
}
