package com.urbanvogue.payment_service.service;

import org.springframework.scheduling.annotation.Scheduled;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.urbanvogue.payment_service.config.MessagingConfig;
import com.urbanvogue.payment_service.dto.PaymentCompletedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.urbanvogue.payment_service.dto.PaymentRequest;
import com.urbanvogue.payment_service.dto.PaymentResponse;
import com.urbanvogue.payment_service.model.Payment;
import com.urbanvogue.payment_service.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    public PaymentService(PaymentRepository paymentRepository, RabbitTemplate rabbitTemplate) {
        this.paymentRepository = paymentRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public PaymentResponse processPayment(PaymentRequest request) {

        String transactionId = UUID.randomUUID().toString();

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl + "?orderId=" + request.getOrderId())
                .addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("usd")
                        .setUnitAmount((long) (request.getAmount() * 100))
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName("UrbanVogue Order #" + request.getOrderId())
                            .build())
                        .build())
                    .build())
                .build();

            Session session = Session.create(params);

            Payment payment = new Payment();
            payment.setOrderId(request.getOrderId());
            payment.setAmount(request.getAmount());
            payment.setPaymentMethod("STRIPE");
            payment.setPaymentStatus("PENDING");
            payment.setTransactionId(transactionId);
            payment.setStripeSessionId(session.getId());
            payment.setCreatedAt(LocalDateTime.now());

            paymentRepository.save(payment);

            return new PaymentResponse(transactionId, "PENDING", session.getUrl());

        } catch (StripeException e) {
            throw new RuntimeException("Error communicating with Stripe", e);
        }
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    public void handleWebhookEvent(String sessionId) {
        Payment payment = paymentRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Payment not found for session: " + sessionId));
        
        if ("SUCCESS".equals(payment.getPaymentStatus())) return;

        payment.setPaymentStatus("SUCCESS");
        paymentRepository.save(payment);

        try {
            PaymentCompletedEvent event = new PaymentCompletedEvent(payment.getOrderId(), payment.getAmount());
            rabbitTemplate.convertAndSend(MessagingConfig.EXCHANGE_NAME, MessagingConfig.ROUTING_KEY, event);
            System.out.println("PaymentCompletedEvent published to RabbitMQ for orderId: " + payment.getOrderId());
        } catch (Exception e) {
            System.err.println("Failed to publish PaymentCompletedEvent: " + e.getMessage());
        }
    }

    public void handleCancelEvent(Long orderId) {
        Payment payment = getPaymentByOrderId(orderId);
        if (!"PENDING".equals(payment.getPaymentStatus())) return;
        
        payment.setPaymentStatus("FAILED");
        paymentRepository.save(payment);

        try {
            com.urbanvogue.payment_service.dto.PaymentFailedEvent event = new com.urbanvogue.payment_service.dto.PaymentFailedEvent(orderId);
            rabbitTemplate.convertAndSend(MessagingConfig.EXCHANGE_NAME, "payment.failed", event);
            System.out.println("PaymentFailedEvent published to RabbitMQ for orderId: " + orderId);
        } catch (Exception e) {
            System.err.println("Failed to publish PaymentFailedEvent: " + e.getMessage());
        }
    }

    // Disabled during local dev to stop log spam. Webhooks handle updates anyway!
    @Scheduled(fixedRate = 10000)
    public void pollStripeEvents() {
        try {
            java.util.List<Payment> pending = paymentRepository.findByPaymentStatus("PENDING");
            for (Payment payment : pending) {
                if (payment.getStripeSessionId() == null) continue;
                Session session = Session.retrieve(payment.getStripeSessionId());
                if ("complete".equals(session.getStatus()) && "paid".equals(session.getPaymentStatus())) {
                    System.out.println("AUTOMATIC POLLER: Found completed payment for order " + payment.getOrderId());
                    handleWebhookEvent(session.getId());
                } else if ("expired".equals(session.getStatus())) {
                    System.out.println("AUTOMATIC POLLER: Found expired checkout for order " + payment.getOrderId());
                    handleCancelEvent(payment.getOrderId());
                }
            }
        } catch (Exception e) {
            System.err.println("AUTOMATIC POLLER CRASHED! Exact Reason:");
            e.printStackTrace();
        }
    }
}