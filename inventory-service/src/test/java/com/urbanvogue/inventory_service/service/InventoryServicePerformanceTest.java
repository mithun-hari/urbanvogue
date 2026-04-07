package com.urbanvogue.inventory_service.service;

import com.urbanvogue.inventory_service.model.Inventory;
import com.urbanvogue.inventory_service.repository.InventoryRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService - Performance Tests")
class InventoryServicePerformanceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("PERF-01: 500 sequential deductions - measures throughput")
    void sequentialDeductions_MeasureThroughput() {
        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProductId(10L);
        inventory.setAvailableQuantity(100000);
        inventory.setReservedQuantity(0);

        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        long start = System.nanoTime();
        for (int i = 0; i < 500; i++) {
            inventory.setAvailableQuantity(100000); // reset for each call
            inventoryService.deductInventory(10L, 1);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        double throughput = (500.0 * 1000) / elapsedMs;

        System.out.println("  [TIME] 500 sequential deductions in " + elapsedMs + "ms");
        System.out.println("  [STAT] Throughput: " + String.format("%.0f", throughput) + " deductions/sec");

        assertTrue(elapsedMs < 2000, "500 deductions took " + elapsedMs + "ms, expected < 2000ms");
    }

    @Test
    @DisplayName("PERF-02: 50 concurrent deductions - race condition stress test")
    void concurrentDeductions_StressTest() throws InterruptedException {
        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProductId(10L);
        inventory.setAvailableQuantity(100000);
        inventory.setReservedQuantity(0);

        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
        lenient().when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        int concurrentOps = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startGate = new CountDownLatch(1);  // ensures all threads start at once
        CountDownLatch doneLatch = new CountDownLatch(concurrentOps);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        CopyOnWriteArrayList<Long> times = new CopyOnWriteArrayList<>();

        for (int i = 0; i < concurrentOps; i++) {
            executor.submit(() -> {
                try {
                    startGate.await(); // all threads wait here
                    long t = System.nanoTime();
                    inventoryService.deductInventory(10L, 1);
                    times.add((System.nanoTime() - t) / 1_000_000);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        long start = System.nanoTime();
        startGate.countDown(); // GO!
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        long totalMs = (System.nanoTime() - start) / 1_000_000;

        double avgMs = times.stream().mapToLong(Long::longValue).average().orElse(0);
        times.sort(Long::compareTo);
        long p95 = times.isEmpty() ? 0 : times.get((int) (times.size() * 0.95));

        System.out.println("\n  ==============================================");
        System.out.println("  CONCURRENT INVENTORY DEDUCTION RESULTS");
        System.out.println("  ==============================================");
        System.out.println("  Concurrent ops:    " + concurrentOps);
        System.out.println("  Successful:        " + successCount.get());
        System.out.println("  Failed:            " + failCount.get());
        System.out.println("  Total time:        " + totalMs + "ms");
        System.out.println("  Avg response:      " + String.format("%.2f", avgMs) + "ms");
        System.out.println("  P95 response:      " + p95 + "ms");
        System.out.println("  Throughput:        " + String.format("%.0f", (successCount.get() * 1000.0) / totalMs) + " ops/sec");
        System.out.println("  ==============================================\n");

        assertEquals(concurrentOps, successCount.get());
    }

    @Test
    @DisplayName("PERF-03: 1000 inventory lookups - read performance")
    void rapidInventoryLookups() {
        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProductId(10L);
        inventory.setAvailableQuantity(500);
        inventory.setReservedQuantity(10);

        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

        int lookupCount = 1000;
        long start = System.nanoTime();
        for (int i = 0; i < lookupCount; i++) {
            inventoryService.getInventoryByProductId(10L);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("  [TIME] " + lookupCount + " inventory lookups in " + elapsedMs + "ms");
        System.out.println("  [STAT] Throughput: " + String.format("%.0f", (lookupCount * 1000.0) / Math.max(elapsedMs, 1)) + " reads/sec");

        assertTrue(elapsedMs < 1000);
    }

    @Test
    @DisplayName("PERF-04: Mixed read/write workload - 70% reads, 30% writes")
    void mixedWorkload_ReadWriteRatio() throws InterruptedException {
        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProductId(10L);
        inventory.setAvailableQuantity(100000);
        inventory.setReservedQuantity(0);

        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
        lenient().when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        int totalOps = 100;
        int reads = 70;
        int writes = 30;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(totalOps);
        AtomicInteger readSuccess = new AtomicInteger(0);
        AtomicInteger writeSuccess = new AtomicInteger(0);

        long start = System.nanoTime();

        // Submit reads
        for (int i = 0; i < reads; i++) {
            executor.submit(() -> {
                try {
                    inventoryService.getInventoryByProductId(10L);
                    readSuccess.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Submit writes
        for (int i = 0; i < writes; i++) {
            executor.submit(() -> {
                try {
                    inventoryService.deductInventory(10L, 1);
                    writeSuccess.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        long totalMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("\n  ==============================================");
        System.out.println("  MIXED WORKLOAD RESULTS (70R/30W)");
        System.out.println("  ==============================================");
        System.out.println("  Read success:      " + readSuccess.get() + "/" + reads);
        System.out.println("  Write success:     " + writeSuccess.get() + "/" + writes);
        System.out.println("  Total time:        " + totalMs + "ms");
        System.out.println("  Combined ops/sec:  " + String.format("%.0f", (totalOps * 1000.0) / totalMs));
        System.out.println("  ==============================================\n");

        assertEquals(reads, readSuccess.get());
        assertEquals(writes, writeSuccess.get());
    }
}
