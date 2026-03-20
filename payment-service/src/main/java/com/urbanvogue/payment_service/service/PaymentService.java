package com.urbanvogue.payment_service.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.urbanvogue.payment_service.client.NotificationClient;
import com.urbanvogue.payment_service.client.OrderClient;
import com.urbanvogue.payment_service.dto.EmailRequest;
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
    private final OrderClient orderClient;
    private final NotificationClient notificationClient;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    public PaymentService(PaymentRepository paymentRepository, OrderClient orderClient, NotificationClient notificationClient) {
        this.paymentRepository = paymentRepository;
        this.orderClient = orderClient;
        this.notificationClient = notificationClient;
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
                .setCancelUrl(cancelUrl)
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
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    public void handleWebhookEvent(String sessionId) {
        Payment payment = paymentRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Payment not found for session: " + sessionId));
        
        payment.setPaymentStatus("SUCCESS");
        paymentRepository.save(payment);

        try {
            // Tell Order Service to update status
            orderClient.updateOrderStatus(payment.getOrderId(), "PAID");

            // Tell Notification Service to send an email
            EmailRequest emailReq = new EmailRequest();
            emailReq.setTo("customer@example.com"); // In a real app, we'd fetch user email via Order Service
            emailReq.setSubject("UrbanVogue Receipt: Order #" + payment.getOrderId());
            emailReq.setBody("Thank you for your purchase! Your payment of $" + payment.getAmount() + " was successful.");
            notificationClient.sendEmail(emailReq);

        } catch (Exception e) {
            System.err.println("Failed to notify downstream services via Feign: " + e.getMessage());
        }
    }
}