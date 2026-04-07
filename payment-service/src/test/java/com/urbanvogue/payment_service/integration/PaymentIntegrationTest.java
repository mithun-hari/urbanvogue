package com.urbanvogue.payment_service.integration;

import com.urbanvogue.payment_service.model.Payment;
import com.urbanvogue.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:payment-testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("PaymentService - Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
    }

    private Payment seedPayment(Long orderId, String status, String sessionId) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(99.99);
        payment.setPaymentMethod("STRIPE");
        payment.setPaymentStatus(status);
        payment.setTransactionId("txn-" + orderId);
        payment.setStripeSessionId(sessionId);
        payment.setCreatedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Test
    @Order(1)
    @DisplayName("INT-01: GET /payments/order/{orderId} - Fetch payment from real DB")
    @WithMockUser
    void getPaymentByOrderId_ShouldReturnFromDatabase() throws Exception {
        seedPayment(1001L, "PENDING", "cs_test_1001");

        mockMvc.perform(get("/payments/order/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1001))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(99.99));

        System.out.println("  [PASS] INT-01: Payment fetched from DB for orderId=1001");
    }

    @Test
    @Order(2)
    @DisplayName("INT-02: POST /payments/mock-webhook/{orderId} - Webhook updates DB status")
    @WithMockUser
    void mockWebhook_ShouldUpdatePaymentStatusInDatabase() throws Exception {
        seedPayment(2001L, "PENDING", "cs_test_2001");

        mockMvc.perform(post("/payments/mock-webhook/2001"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MOCK WEBHOOK SUCCESS")));

        // Verify DB was updated to SUCCESS
        Payment updated = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(2001L).orElseThrow();
        assertEquals("SUCCESS", updated.getPaymentStatus(), "Payment status should be SUCCESS after webhook");
        System.out.println("  [PASS] INT-02: Webhook updated payment status to SUCCESS in DB");
    }

    @Test
    @Order(3)
    @DisplayName("INT-03: POST /payments/mock-webhook - Idempotency: second call should not fail")
    @WithMockUser
    void mockWebhook_CalledTwice_ShouldBeIdempotent() throws Exception {
        seedPayment(3001L, "PENDING", "cs_test_3001");

        // First call
        mockMvc.perform(post("/payments/mock-webhook/3001"))
                .andExpect(status().isOk());

        // Second call (already SUCCESS) - should not crash
        mockMvc.perform(post("/payments/mock-webhook/3001"))
                .andExpect(status().isOk());

        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(3001L).orElseThrow();
        assertEquals("SUCCESS", payment.getPaymentStatus());
        System.out.println("  [PASS] INT-03: Idempotent webhook - second call succeeded without side effects");
    }

    @Test
    @Order(4)
    @DisplayName("INT-04: GET /payments/order/{orderId} - Non-existent order returns error")
    @WithMockUser
    void getPayment_NonExistent_ShouldReturn500() throws Exception {
        mockMvc.perform(get("/payments/order/99999"))
                .andExpect(status().is5xxServerError());

        System.out.println("  [PASS] INT-04: Non-existent orderId correctly returned error");
    }

    @Test
    @Order(5)
    @DisplayName("INT-05: GET /payments/cancel - Cancel updates payment status to FAILED")
    @WithMockUser
    void cancelPayment_ShouldUpdateStatusToFailed() throws Exception {
        seedPayment(5001L, "PENDING", "cs_test_5001");

        mockMvc.perform(get("/payments/cancel")
                        .param("orderId", "5001"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Payment Cancelled")));

        // Verify DB updated to FAILED
        Payment updated = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(5001L).orElseThrow();
        assertEquals("FAILED", updated.getPaymentStatus(), "Cancelled payment should be FAILED");
        System.out.println("  [PASS] INT-05: Payment cancelled, status updated to FAILED in DB");
    }
}
