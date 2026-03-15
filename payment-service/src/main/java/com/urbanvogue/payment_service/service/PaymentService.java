package com.urbanvogue.payment_service.service;

import com.urbanvogue.payment_service.dto.PaymentRequest;
import com.urbanvogue.payment_service.dto.PaymentResponse;
import com.urbanvogue.payment_service.model.Payment;
import com.urbanvogue.payment_service.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse processPayment(PaymentRequest request) {

        String transactionId = UUID.randomUUID().toString();

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentStatus("SUCCESS");
        payment.setTransactionId(transactionId);
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);


        return new PaymentResponse(transactionId, "SUCCESS");
    }
    public Payment getPaymentByOrderId(Long orderId) {

        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

}