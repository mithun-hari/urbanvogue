package com.urbanvogue.payment_service.repository;

import com.urbanvogue.payment_service.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    java.util.List<Payment> findByPaymentStatus(String paymentStatus);

}