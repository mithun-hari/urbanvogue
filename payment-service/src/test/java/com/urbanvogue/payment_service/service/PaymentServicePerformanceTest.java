package com.urbanvogue.payment_service.service;

import com.urbanvogue.payment_service.config.MessagingConfig;
import com.urbanvogue.payment_service.model.Payment;
import com.urbanvogue.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService - Performance Tests")
class PaymentServicePerformanceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private Payment createPendingPayment(Long orderId, String sessionId) {
        Payment p = new Payment();
        p.setId(orderId);
        p.setOrderId(orderId);
        p.setAmount(99.99);
        p.setPaymentMethod("STRIPE");
        p.setPaymentStatus("PENDING");
        p.setTransactionId("txn-" + orderId);
        p.setStripeSessionId(sessionId);
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }

    @Test
    @DisplayName("PERF-01: 100 sequential webhook events - measures processing speed")
    void sequentialWebhookProcessing() {
        int count = 100;
        long start = System.nanoTime();

        for (int i = 0; i < count; i++) {
            String sessionId = "cs_test_" + i;
            Payment payment = createPendingPayment((long) i, sessionId);
            when(paymentRepository.findByStripeSessionId(sessionId)).thenReturn(Optional.of(payment));

            paymentService.handleWebhookEvent(sessionId);
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("  [TIME] " + count + " webhook events processed in " + elapsedMs + "ms");
        System.out.println("  [STAT] Throughput: " + String.format("%.0f", (count * 1000.0) / elapsedMs) + " webhooks/sec");

        verify(rabbitTemplate, times(count)).convertAndSend(
                eq(MessagingConfig.EXCHANGE_NAME), eq(MessagingConfig.ROUTING_KEY), any(Object.class));

        assertTrue(elapsedMs < 3000);
    }

    @Test
    @DisplayName("PERF-02: 50 concurrent webhook events - parallel processing")
    void concurrentWebhookProcessing() throws InterruptedException {
        int concurrentEvents = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrentEvents);
        AtomicInteger success = new AtomicInteger(0);
        CopyOnWriteArrayList<Long> times = new CopyOnWriteArrayList<>();

        for (int i = 0; i < concurrentEvents; i++) {
            final int idx = i;
            String sessionId = "cs_concurrent_" + idx;
            Payment payment = createPendingPayment((long) idx, sessionId);
            lenient().when(paymentRepository.findByStripeSessionId(sessionId)).thenReturn(Optional.of(payment));

            executor.submit(() -> {
                try {
                    startGate.await();
                    long t = System.nanoTime();
                    paymentService.handleWebhookEvent(sessionId);
                    times.add((System.nanoTime() - t) / 1_000_000);
                    success.incrementAndGet();
                } catch (Exception e) {
                    // count as failed
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        long start = System.nanoTime();
        startGate.countDown();
        doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();
        long totalMs = (System.nanoTime() - start) / 1_000_000;

        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
        times.sort(Long::compareTo);
        long p95 = times.isEmpty() ? 0 : times.get((int) (times.size() * 0.95));

        System.out.println("\n  ==============================================");
        System.out.println("  CONCURRENT WEBHOOK PROCESSING RESULTS");
        System.out.println("  ==============================================");
        System.out.println("  Concurrent events: " + concurrentEvents);
        System.out.println("  Successful:        " + success.get());
        System.out.println("  Total time:        " + totalMs + "ms");
        System.out.println("  Avg response:      " + String.format("%.2f", avg) + "ms");
        System.out.println("  P95 response:      " + p95 + "ms");
        System.out.println("  Throughput:        " + String.format("%.0f", (success.get() * 1000.0) / Math.max(totalMs, 1)) + " events/sec");
        System.out.println("  ==============================================\n");

        assertEquals(concurrentEvents, success.get());
    }

    @Test
    @DisplayName("PERF-03: Idempotency under load - 100 duplicate webhook calls")
    void idempotencyUnderLoad() {
        String sessionId = "cs_idempotent_test";
        Payment payment = createPendingPayment(1L, sessionId);
        when(paymentRepository.findByStripeSessionId(sessionId)).thenReturn(Optional.of(payment));

        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            paymentService.handleWebhookEvent(sessionId);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Only first call should save and publish (status changes to SUCCESS), rest should skip
        verify(paymentRepository, times(1)).save(any());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));

        System.out.println("  [TIME] 100 duplicate webhook calls in " + elapsedMs + "ms");
        System.out.println("  [PASS] Idempotency verified: only 1 save + 1 publish out of 100 calls");
        System.out.println("  [STAT] Throughput: " + String.format("%.0f", (100.0 * 1000) / elapsedMs) + " calls/sec");
    }
}
