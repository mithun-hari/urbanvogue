package com.urbanvogue.order_service.controller;

import com.urbanvogue.order_service.dto.CreateOrderRequest;
import com.urbanvogue.order_service.dto.OrderResponse;
import com.urbanvogue.order_service.model.Order;
import com.urbanvogue.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public OrderResponse createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestBody CreateOrderRequest request) {

        return orderService.createOrder(userId, userEmail, request);
    }
    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable("id") Long id) {
        return orderService.getOrderById(id);
    }
    @GetMapping("/user/{userId}")
    public List<Order> getOrdersByUser(@PathVariable("userId") Long userId) {
        return orderService.getOrdersByUser(userId);
    }

    @PutMapping("/{id}/status")
    public void updateOrderStatus(@PathVariable("id") Long id, @RequestParam("status") String status) {
        orderService.updateOrderStatus(id, status);
    }
}