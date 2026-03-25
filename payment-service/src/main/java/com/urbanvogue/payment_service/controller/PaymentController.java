package com.urbanvogue.payment_service.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.urbanvogue.payment_service.dto.PaymentRequest;
import com.urbanvogue.payment_service.dto.PaymentResponse;
import com.urbanvogue.payment_service.model.Payment;
import com.urbanvogue.payment_service.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentResponse processPayment(@Valid @RequestBody PaymentRequest request) {
        return paymentService.processPayment(request);
    }

    @GetMapping("/order/{orderId}")
    public Payment getPaymentByOrderId(@PathVariable Long orderId) {
        return paymentService.getPaymentByOrderId(orderId);
    }

    @GetMapping("/cancel")
    public ResponseEntity<String> cancelPayment(@RequestParam Long orderId) {
        paymentService.handleCancelEvent(orderId);
        return ResponseEntity.ok("Payment Cancelled safely. Inventory has been manually restored under the Saga Pattern.");
    }

    @PostMapping("/mock-webhook/{orderId}")
    public ResponseEntity<String> mockStripeWebhook(@PathVariable Long orderId) {
        try {
            Payment payment = paymentService.getPaymentByOrderId(orderId);
            paymentService.handleWebhookEvent(payment.getStripeSessionId());
            return ResponseEntity.ok("MOCK WEBHOOK SUCCESS: Order " + orderId + " is now PAID, RabbitMQ event fired, and Email is sending!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Mock Webhook Failed: " + e.getMessage());
        }
    }


    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event = null;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            try {
                EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
                StripeObject stripeObject = dataObjectDeserializer.deserializeUnsafe();
                
                if (stripeObject instanceof Session) {
                    Session session = (Session) stripeObject;
                    paymentService.handleWebhookEvent(session.getId());
                } else {
                    System.err.println("Webhook checkout.session.completed object is not a Session!");
                }
            } catch (Exception e) {
                System.err.println("Stripe Webhook Deserialization Failed: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Deserialization failed");
            }
        }

        return ResponseEntity.ok("Success");
    }
}