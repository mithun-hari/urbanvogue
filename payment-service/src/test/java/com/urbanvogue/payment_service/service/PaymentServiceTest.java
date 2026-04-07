package com.urbanvogue.payment_service.service;

import com.urbanvogue.payment_service.config.MessagingConfig;
import com.urbanvogue.payment_service.dto.PaymentCompletedEvent;
import com.urbanvogue.payment_service.model.Payment;
import com.urbanvogue.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private Payment pendingPayment;

    @BeforeEach
    void setUp() {
        pendingPayment = new Payment();
        pendingPayment.setId(1L);
        pendingPayment.setOrderId(100L);
        pendingPayment.setAmount(250.0);
        pendingPayment.setPaymentMethod("STRIPE");
        pendingPayment.setPaymentStatus("PENDING");
        pendingPayment.setTransactionId("txn-123");
        pendingPayment.setStripeSessionId("cs_test_abc");
        pendingPayment.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("handleWebhookEvent - Updates status to SUCCESS and publishes RabbitMQ event")
    void handleWebhookEvent_Success() {
        when(paymentRepository.findByStripeSessionId("cs_test_abc"))
                .thenReturn(Optional.of(pendingPayment));

        paymentService.handleWebhookEvent("cs_test_abc");

        // Verify status updated
        assertEquals("SUCCESS", pendingPayment.getPaymentStatus());
        verify(paymentRepository).save(pendingPayment);

        // Verify RabbitMQ event published with correct data
        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(MessagingConfig.EXCHANGE_NAME),
                eq(MessagingConfig.ROUTING_KEY),
                eventCaptor.capture());

        PaymentCompletedEvent event = eventCaptor.getValue();
        assertEquals(100L, event.getOrderId());
        assertEquals(250.0, event.getAmount());
    }

    @Test
    @DisplayName("handleWebhookEvent - Already SUCCESS: skips update (idempotent)")
    void handleWebhookEvent_AlreadySuccess_Skips() {
        pendingPayment.setPaymentStatus("SUCCESS");
        when(paymentRepository.findByStripeSessionId("cs_test_abc"))
                .thenReturn(Optional.of(pendingPayment));

        paymentService.handleWebhookEvent("cs_test_abc");

        // Should not save or publish since already SUCCESS
        verify(paymentRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("handleWebhookEvent - Payment not found throws RuntimeException")
    void handleWebhookEvent_PaymentNotFound_Throws() {
        when(paymentRepository.findByStripeSessionId("unknown_session"))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                paymentService.handleWebhookEvent("unknown_session"));

        assertTrue(ex.getMessage().contains("Payment not found"));
    }

    @Test
    @DisplayName("handleCancelEvent - Cancels PENDING payment and sets FAILED")
    void handleCancelEvent_Success() {
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(pendingPayment));

        paymentService.handleCancelEvent(100L);

        assertEquals("FAILED", pendingPayment.getPaymentStatus());
        verify(paymentRepository).save(pendingPayment);
    }

    @Test
    @DisplayName("handleCancelEvent - Non-PENDING payment: does nothing")
    void handleCancelEvent_NotPending_Skips() {
        pendingPayment.setPaymentStatus("SUCCESS");
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(pendingPayment));

        paymentService.handleCancelEvent(100L);

        // Status should remain SUCCESS, no save
        assertEquals("SUCCESS", pendingPayment.getPaymentStatus());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("getPaymentByOrderId - Not found throws RuntimeException")
    void getPaymentByOrderId_NotFound_Throws() {
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(999L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                paymentService.getPaymentByOrderId(999L));

        assertEquals("Payment not found", ex.getMessage());
    }
}
