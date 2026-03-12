package com.urbanvogue.order_service.service;

import com.urbanvogue.order_service.client.InventoryClient;
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

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    public OrderResponse createOrder(CreateOrderRequest request) {

        Order order = new Order();

        order.setUserId(request.getUserId());
        order.setStatus("PLACED");
        order.setCreatedAt(LocalDateTime.now());

        var items = request.getItems().stream().map(itemRequest -> {

            ProductResponse product = productClient.getProduct(itemRequest.getProductId());

            // Check inventory before creating order item
            InventoryResponse inventory =
                    inventoryClient.getInventory(itemRequest.getProductId());

            if (inventory.getAvailableQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: "
                        + itemRequest.getProductId());
            }

            OrderItem item = new OrderItem();
            item.setProductId(itemRequest.getProductId());
            item.setQuantity(itemRequest.getQuantity());
            item.setPrice(product.getPrice());
            item.setOrder(order);

            return item;

        }).collect(Collectors.toList());

        order.setItems(items);

        // 🔹 ADDED: Calculate total order amount
        double totalAmount = items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        order.setTotalAmount(totalAmount);
        // 🔹 END OF ADDED PART

        Order savedOrder = orderRepository.save(order);

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

}