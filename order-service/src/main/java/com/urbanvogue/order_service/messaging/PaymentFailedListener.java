package com.urbanvogue.order_service.messaging;

import com.urbanvogue.order_service.dto.PaymentFailedEvent;
import com.urbanvogue.order_service.repository.OrderRepository;
import com.urbanvogue.order_service.client.InventoryClient;
import com.urbanvogue.order_service.model.Order;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import java.util.Optional;

@Component
public class PaymentFailedListener {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public PaymentFailedListener(OrderRepository orderRepository, InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "payment_failed_queue", durable = "true"),
            exchange = @Exchange(value = "payment_exchange", type = "topic"),
            key = "payment.failed"
    ))
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        System.out.println("SAGA REVERSAL: Received PaymentFailedEvent for orderId: " + event.getOrderId());
        Optional<Order> orderOpt = orderRepository.findById(event.getOrderId());
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if("PAID".equals(order.getStatus())) return; // Safeguard
            
            order.setStatus("FAILED");
            orderRepository.save(order);
            System.out.println("SAGA REVERSAL: Order " + order.getId() + " marked as FAILED.");
            
            // Restore inventory
            for (var item : order.getItems()) {
                inventoryClient.restoreInventory(item.getProductId(), item.getQuantity());
                System.out.println("SAGA REVERSAL: Restored " + item.getQuantity() + " stock for Product " + item.getProductId());
            }
        }
    }
}
