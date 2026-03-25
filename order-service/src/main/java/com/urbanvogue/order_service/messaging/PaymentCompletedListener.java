package com.urbanvogue.order_service.messaging;

import com.urbanvogue.order_service.config.MessagingConfig;
import com.urbanvogue.order_service.dto.OrderPaidEvent;
import com.urbanvogue.order_service.dto.PaymentCompletedEvent;
import com.urbanvogue.order_service.model.Order;
import com.urbanvogue.order_service.repository.OrderRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PaymentCompletedListener {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    public PaymentCompletedListener(OrderRepository orderRepository, RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = MessagingConfig.PAYMENT_QUEUE)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        System.out.println("Received PaymentCompletedEvent for orderId: " + event.getOrderId());

        Optional<Order> orderOpt = orderRepository.findById(event.getOrderId());
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus("PAID");
            orderRepository.save(order);
            System.out.println("Order status updated to PAID for orderId: " + event.getOrderId());

            // Publish OrderPaidEvent to trigger email in Notification Service
            OrderPaidEvent orderPaidEvent = new OrderPaidEvent(order.getId(), event.getAmount(), order.getUserEmail());
            rabbitTemplate.convertAndSend(MessagingConfig.ORDER_EXCHANGE, "order.paid", orderPaidEvent);
            System.out.println("Published OrderPaidEvent for orderId: " + order.getId());

        } else {
            System.err.println("Order not found inside listener for order id: " + event.getOrderId());
        }
    }
}
