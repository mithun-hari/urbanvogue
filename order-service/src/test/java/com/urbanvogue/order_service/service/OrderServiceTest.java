package com.urbanvogue.order_service.service;

import com.urbanvogue.order_service.client.InventoryClient;
import com.urbanvogue.order_service.client.PaymentClient;
import com.urbanvogue.order_service.client.ProductClient;
import com.urbanvogue.order_service.dto.*;
import com.urbanvogue.order_service.model.Order;
import com.urbanvogue.order_service.model.OrderItem;
import com.urbanvogue.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest createOrderRequest;
    private ProductResponse productResponse;
    private InventoryResponse inventoryResponse;

    @BeforeEach
    void setUp() {
        // Set up a single-item order request
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setItems(List.of(itemRequest));

        // Product costs $50.0
        productResponse = new ProductResponse();
        productResponse.setId(1L);
        productResponse.setName("Test Sneaker");
        productResponse.setPrice(50.0);

        // 10 units available
        inventoryResponse = new InventoryResponse();
        inventoryResponse.setProductId(1L);
        inventoryResponse.setAvailableQuantity(10);
    }

    @Test
    @DisplayName("createOrder - Happy path: order created with PENDING status")
    void createOrder_Success() {
        // Arrange
        when(productClient.getProduct(1L)).thenReturn(productResponse);
        when(inventoryClient.getInventory(1L)).thenReturn(inventoryResponse);
        when(paymentClient.processPayment(anyLong(), anyDouble()))
                .thenReturn("{\"checkoutUrl\":\"https://checkout.stripe.com/test\"}");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // Act
        OrderResponse response = orderService.createOrder(100L, "test@example.com", createOrderRequest);

        // Assert
        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertEquals(100.0, response.getTotalAmount()); // 50 * 2
        assertEquals("https://checkout.stripe.com/test", response.getPaymentUrl());

        verify(inventoryClient).deductInventory(1L, 2);
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder - Insufficient stock throws RuntimeException")
    void createOrder_InsufficientStock_ThrowsException() {
        // Arrange: only 1 unit available, but we want 2
        inventoryResponse.setAvailableQuantity(1);

        when(productClient.getProduct(1L)).thenReturn(productResponse);
        when(inventoryClient.getInventory(1L)).thenReturn(inventoryResponse);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                orderService.createOrder(100L, "test@example.com", createOrderRequest));

        assertTrue(ex.getMessage().contains("Insufficient stock"));
        verify(inventoryClient, never()).deductInventory(anyLong(), anyInt());
    }

    @Test
    @DisplayName("createOrder - Payment failure sets FAILED status and restores inventory")
    void createOrder_PaymentFails_RestoresInventory() {
        // Arrange
        when(productClient.getProduct(1L)).thenReturn(productResponse);
        when(inventoryClient.getInventory(1L)).thenReturn(inventoryResponse);
        when(paymentClient.processPayment(anyLong(), anyDouble()))
                .thenThrow(new RuntimeException("Stripe error"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // Act
        OrderResponse response = orderService.createOrder(100L, "test@example.com", createOrderRequest);

        // Assert
        assertEquals("FAILED", response.getStatus());
        verify(inventoryClient).restoreInventory(1L, 2);
    }

    @Test
    @DisplayName("createOrder - Calculates total correctly for multiple items")
    void createOrder_CalculatesTotalCorrectly() {
        // Arrange: 2 different items
        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId(1L);
        item1.setQuantity(2); // 2 x $50 = $100

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId(2L);
        item2.setQuantity(3); // 3 x $30 = $90

        createOrderRequest.setItems(List.of(item1, item2));

        ProductResponse product2 = new ProductResponse();
        product2.setId(2L);
        product2.setName("T-Shirt");
        product2.setPrice(30.0);

        InventoryResponse inv2 = new InventoryResponse();
        inv2.setProductId(2L);
        inv2.setAvailableQuantity(20);

        when(productClient.getProduct(1L)).thenReturn(productResponse);
        when(productClient.getProduct(2L)).thenReturn(product2);
        when(inventoryClient.getInventory(1L)).thenReturn(inventoryResponse);
        when(inventoryClient.getInventory(2L)).thenReturn(inv2);
        when(paymentClient.processPayment(anyLong(), anyDouble())).thenReturn("{}");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // Act
        OrderResponse response = orderService.createOrder(100L, "test@example.com", createOrderRequest);

        // Assert: $100 + $90 = $190
        assertEquals(190.0, response.getTotalAmount());
    }

    @Test
    @DisplayName("getOrderById - Returns order when found")
    void getOrderById_Success() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus("PENDING");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Order result = orderService.getOrderById(1L);

        assertEquals(1L, result.getId());
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    @DisplayName("getOrderById - Throws when order not found")
    void getOrderById_NotFound_ThrowsException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                orderService.getOrderById(999L));

        assertEquals("Order not found", ex.getMessage());
    }

    @Test
    @DisplayName("updateOrderStatus - Updates and saves")
    void updateOrderStatus_Success() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus("PENDING");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.updateOrderStatus(1L, "SUCCESS");

        assertEquals("SUCCESS", order.getStatus());
        verify(orderRepository).save(order);
    }
}
