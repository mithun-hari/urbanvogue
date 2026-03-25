package com.urbanvogue.notification_service.messaging;

import com.urbanvogue.notification_service.config.MessagingConfig;
import com.urbanvogue.notification_service.dto.EmailRequest;
import com.urbanvogue.notification_service.dto.OrderPaidEvent;
import com.urbanvogue.notification_service.service.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPaidListener {

    private final EmailService emailService;

    public OrderPaidListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = MessagingConfig.ORDER_PAID_QUEUE)
    public void handleOrderPaidEvent(OrderPaidEvent event) {
        System.out.println("Received OrderPaidEvent for orderId: " + event.getOrderId() + ", sending email...");

        EmailRequest emailReq = new EmailRequest();
        emailReq.setTo(event.getUserEmail());
        emailReq.setSubject("UrbanVogue Receipt: Order #" + event.getOrderId());
        emailReq.setBody("Thank you for your purchase! Your payment of $" + event.getAmount() + " was successful.");

        emailService.sendEmail(emailReq);
        System.out.println("Email requested successfully via RabbitMQ message.");
    }
}
