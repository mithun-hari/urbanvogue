package com.urbanvogue.payment_service.controller;

import com.urbanvogue.payment_service.dto.PaymentRequest;
import com.urbanvogue.payment_service.dto.PaymentResponse;
import com.urbanvogue.payment_service.model.Payment;
import com.urbanvogue.payment_service.service.PaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentResponse processPayment(@RequestBody PaymentRequest request) {
        return paymentService.processPayment(request);
    }
    @GetMapping("/order/{orderId}")
    public Payment getPaymentByOrderId(@PathVariable Long orderId) {
        return paymentService.getPaymentByOrderId(orderId);
    }
}