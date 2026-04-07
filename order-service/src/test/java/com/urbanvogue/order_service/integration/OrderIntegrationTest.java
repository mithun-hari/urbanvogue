package com.urbanvogue.order_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanvogue.order_service.client.InventoryClient;
import com.urbanvogue.order_service.client.PaymentClient;
import com.urbanvogue.order_service.client.ProductClient;
import com.urbanvogue.order_service.dto.*;
import com.urbanvogue.order_service.model.Order;
import com.urbanvogue.order_service.repository.OrderRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("OrderService - Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock external Feign clients (cross-service dependencies)
    @MockitoBean
    private ProductClient productClient;

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private PaymentClient paymentClient;

    @MockitoBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        // Setup mock responses for Feign clients
        ProductResponse product = new ProductResponse();
        product.setId(1L);
        product.setName("Test Sneaker");
        product.setPrice(99.99);
        when(productClient.getProduct(anyLong())).thenReturn(product);

        InventoryResponse inventory = new InventoryResponse();
        inventory.setProductId(1L);
        inventory.setAvailableQuantity(1000);
        when(inventoryClient.getInventory(anyLong())).thenReturn(inventory);

        when(paymentClient.processPayment(anyLong(), anyDouble()))
                .thenReturn("{\"checkoutUrl\":\"https://checkout.stripe.com/test\"}");
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("INT-01: POST /orders - Full order creation flow with real DB")
    @WithMockUser
    void createOrder_ShouldPersistToDatabase() throws Exception {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(item));

        mockMvc.perform(post("/orders")
                        .header("X-User-Id", "42")
                        .header("X-User-Email", "mithun@urbanvogue.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(199.98));

        // Verify order was persisted in H2 DB
        assertEquals(1, orderRepository.count(), "One order should be in DB");
        Order savedOrder = orderRepository.findAll().get(0);
        assertEquals(42L, savedOrder.getUserId());
        assertEquals("mithun@urbanvogue.com", savedOrder.getUserEmail());
        assertEquals("PENDING", savedOrder.getStatus());
        System.out.println("  [PASS] INT-01: Order created and persisted with id=" + savedOrder.getId());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("INT-02: GET /orders/{id} - Fetch order from real DB")
    @WithMockUser
    void getOrderById_ShouldReturnFromDatabase() throws Exception {
        // Seed an order directly in DB
        Order order = Order.builder()
                .userId(10L)
                .userEmail("test@test.com")
                .totalAmount(50.0)
                .status("PENDING")
                .build();
        order = orderRepository.save(order);

        mockMvc.perform(get("/orders/" + order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(50.0));

        System.out.println("  [PASS] INT-02: Order fetched from DB with id=" + order.getId());
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("INT-03: GET /orders/user/{userId} - User order history from DB")
    @WithMockUser
    void getOrdersByUser_ShouldReturnUserHistory() throws Exception {
        // Seed multiple orders for userId=77
        orderRepository.save(Order.builder().userId(77L).userEmail("u@t.com").totalAmount(10.0).status("PENDING").build());
        orderRepository.save(Order.builder().userId(77L).userEmail("u@t.com").totalAmount(20.0).status("SUCCESS").build());
        orderRepository.save(Order.builder().userId(99L).userEmail("x@t.com").totalAmount(30.0).status("PENDING").build());

        mockMvc.perform(get("/orders/user/77"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        System.out.println("  [PASS] INT-03: User 77 has 2 orders, user 99 has 1 (not returned)");
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("INT-04: PUT /orders/{id}/status - Update order status in DB")
    @WithMockUser
    void updateOrderStatus_ShouldPersistNewStatus() throws Exception {
        Order order = Order.builder()
                .userId(5L)
                .userEmail("status@test.com")
                .totalAmount(75.0)
                .status("PENDING")
                .build();
        order = orderRepository.save(order);

        mockMvc.perform(put("/orders/" + order.getId() + "/status")
                        .param("status", "PAID"))
                .andExpect(status().isOk());

        // Verify DB was updated
        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals("PAID", updated.getStatus(), "Status should be PAID now");
        System.out.println("  [PASS] INT-04: Order status updated from PENDING to PAID in DB");
    }
}
