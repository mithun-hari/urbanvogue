package com.urbanvogue.order_service.service;

import com.urbanvogue.order_service.client.InventoryClient;
import com.urbanvogue.order_service.client.PaymentClient;
import com.urbanvogue.order_service.client.ProductClient;
import com.urbanvogue.order_service.dto.CreateOrderRequest;
import com.urbanvogue.order_service.dto.InventoryResponse;
import com.urbanvogue.order_service.dto.OrderResponse;
import com.urbanvogue.order_service.dto.ProductResponse;
import com.urbanvogue.order_service.model.Order;
import com.urbanvogue.order_service.model.OrderItem;
import com.urbanvogue.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public OrderResponse createOrder(CreateOrderRequest request) {

        log.info("Creating order for userId: {}", request.getUserId());

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus("CREATED");
        order.setCreatedAt(LocalDateTime.now());

        var items = request.getItems().stream().map(itemRequest -> {

            log.info("Fetching product details for productId: {}", itemRequest.getProductId());
            ProductResponse product =
                    productClient.getProduct(itemRequest.getProductId());

            log.info("Checking inventory for productId: {}", itemRequest.getProductId());
            InventoryResponse inventory =
                    inventoryClient.getInventory(itemRequest.getProductId());

            if (inventory.getAvailableQuantity() < itemRequest.getQuantity()) {
                log.error("Insufficient stock for productId: {}", itemRequest.getProductId());
                throw new RuntimeException("Insufficient stock for product: "
                        + itemRequest.getProductId());
            }

            log.info("Deducting inventory for productId: {}, quantity: {}",
                    itemRequest.getProductId(), itemRequest.getQuantity());

            inventoryClient.deductInventory(
                    itemRequest.getProductId(),
                    itemRequest.getQuantity()
            );

            OrderItem item = new OrderItem();
            item.setProductId(itemRequest.getProductId());
            item.setQuantity(itemRequest.getQuantity());
            item.setPrice(product.getPrice());
            item.setOrder(order);

            return item;

        }).collect(Collectors.toList());

        order.setItems(items);

        double totalAmount = items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        log.info("Total order amount calculated: {}", totalAmount);

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        try {

            log.info("Processing payment for orderId: {}", savedOrder.getId());
            paymentClient.processPayment(savedOrder.getId(), totalAmount);

            savedOrder.setStatus("PAID");
            log.info("Payment successful for orderId: {}", savedOrder.getId());

        } catch (Exception e) {

            log.error("Payment failed for orderId: {}", savedOrder.getId());

            savedOrder.setStatus("FAILED");

            for (var item : items) {
                inventoryClient.restoreInventory(
                        item.getProductId(),
                        item.getQuantity()
                );
            }
        }

        orderRepository.save(savedOrder);

        log.info("Order completed with status: {}", savedOrder.getStatus());

        return OrderResponse.builder()
                .orderId(savedOrder.getId())
                .totalAmount(savedOrder.getTotalAmount())
                .status(savedOrder.getStatus())
                .build();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public void updateOrderStatus(Long id, String status) {
        Order order = getOrderById(id);
        order.setStatus(status);
        orderRepository.save(order);
        log.info("Order {} status updated to {}", id, status);
    }
}