package com.urbanvogue.order_service.service;

import com.urbanvogue.order_service.client.InventoryClient;
import com.urbanvogue.order_service.client.PaymentClient;
import com.urbanvogue.order_service.client.ProductClient;
import com.urbanvogue.order_service.dto.*;
import com.urbanvogue.order_service.model.Order;
import com.urbanvogue.order_service.repository.OrderRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService - Performance Tests")
class OrderServicePerformanceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(1);

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setItems(List.of(item));

        ProductResponse product = new ProductResponse();
        product.setId(1L);
        product.setName("Sneaker");
        product.setPrice(99.99);

        InventoryResponse inventory = new InventoryResponse();
        inventory.setProductId(1L);
        inventory.setAvailableQuantity(10000);

        lenient().when(productClient.getProduct(anyLong())).thenReturn(product);
        lenient().when(inventoryClient.getInventory(anyLong())).thenReturn(inventory);
        lenient().when(paymentClient.processPayment(anyLong(), anyDouble()))
                .thenReturn("{\"checkoutUrl\":\"https://checkout.stripe.com/test\"}");
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(ThreadLocalRandom.current().nextLong(1, 100000));
            return order;
        });
    }

    @Test
    @DisplayName("PERF-01: Single order creation should complete within 500ms (includes JVM warmup)")
    void singleOrderCreation_ShouldBeUnder500ms() {
        // Warmup call (JVM class-loading + Mockito stub init)
        orderService.createOrder(0L, "warmup@test.com", createOrderRequest);

        long start = System.nanoTime();

        OrderResponse response = orderService.createOrder(1L, "perf@test.com", createOrderRequest);

        long elapsed = (System.nanoTime() - start) / 1_000_000; // ms
        assertNotNull(response);
        System.out.println("  [TIME] Single order creation (post-warmup): " + elapsed + "ms");
        assertTrue(elapsed < 500, "Order creation took " + elapsed + "ms, expected < 500ms");
    }

    @Test
    @DisplayName("PERF-02: 100 sequential orders - measures throughput")
    void sequentialOrders_MeasureThroughput() {
        int totalOrders = 100;
        long start = System.nanoTime();

        for (int i = 0; i < totalOrders; i++) {
            orderService.createOrder((long) i, "user" + i + "@test.com", createOrderRequest);
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        double throughput = (totalOrders * 1000.0) / elapsedMs;

        System.out.println("  [TIME] " + totalOrders + " sequential orders in " + elapsedMs + "ms");
        System.out.println("  [STAT] Throughput: " + String.format("%.1f", throughput) + " orders/sec");

        assertTrue(elapsedMs < 5000, "100 orders took " + elapsedMs + "ms, expected < 5000ms");
    }

    @Test
    @DisplayName("PERF-03: 50 concurrent orders - stress test with thread pool")
    void concurrentOrders_StressTest() throws InterruptedException {
        int concurrentUsers = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();

        long start = System.nanoTime();

        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    long reqStart = System.nanoTime();
                    OrderResponse resp = orderService.createOrder(
                            (long) userId, "user" + userId + "@test.com", createOrderRequest);
                    long reqTime = (System.nanoTime() - reqStart) / 1_000_000;
                    responseTimes.add(reqTime);

                    if (resp != null) successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        long totalMs = (System.nanoTime() - start) / 1_000_000;

        // Calculate stats
        double avgMs = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxMs = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minMs = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        responseTimes.sort(Long::compareTo);
        long p95 = responseTimes.get((int) (responseTimes.size() * 0.95));

        System.out.println("\n  ==============================================");
        System.out.println("  CONCURRENT ORDER CREATION RESULTS");
        System.out.println("  ==============================================");
        System.out.println("  Concurrent users:  " + concurrentUsers);
        System.out.println("  Successful:        " + successCount.get());
        System.out.println("  Failed:            " + failCount.get());
        System.out.println("  Total time:        " + totalMs + "ms");
        System.out.println("  Avg response:      " + String.format("%.1f", avgMs) + "ms");
        System.out.println("  Min response:      " + minMs + "ms");
        System.out.println("  Max response:      " + maxMs + "ms");
        System.out.println("  P95 response:      " + p95 + "ms");
        System.out.println("  Throughput:        " + String.format("%.1f", (successCount.get() * 1000.0) / totalMs) + " req/sec");
        System.out.println("  ==============================================\n");

        assertEquals(concurrentUsers, successCount.get(), "All orders should succeed");
        assertEquals(0, failCount.get(), "No orders should fail");
    }

    @Test
    @DisplayName("PERF-04: getOrderById - 1000 rapid lookups")
    void rapidOrderLookups_1000() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus("PENDING");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            orderService.getOrderById(1L);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("  [TIME] 1000 order lookups in " + elapsedMs + "ms");
        System.out.println("  [STAT] Throughput: " + String.format("%.0f", (1000.0 * 1000) / elapsedMs) + " lookups/sec");

        assertTrue(elapsedMs < 1000, "1000 lookups took " + elapsedMs + "ms, expected < 1000ms");
    }
}
