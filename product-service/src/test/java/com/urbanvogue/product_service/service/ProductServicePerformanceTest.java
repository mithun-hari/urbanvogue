package com.urbanvogue.product_service.service;

import com.urbanvogue.product_service.dto.ProductRequestDTO;
import com.urbanvogue.product_service.dto.ProductResponseDTO;
import com.urbanvogue.product_service.model.Product;
import com.urbanvogue.product_service.repository.ProductRepository;
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
@DisplayName("ProductService - Performance Tests")
class ProductServicePerformanceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("PERF-01: Bulk product creation - 200 products")
    void bulkProductCreation() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(ThreadLocalRandom.current().nextLong(1, 100000));
            return p;
        });

        long start = System.nanoTime();
        for (int i = 0; i < 200; i++) {
            ProductRequestDTO req = new ProductRequestDTO();
            req.setName("Product-" + i);
            req.setDescription("Description for product " + i);
            req.setPrice(10.0 + i);
            req.setImageUrl("https://img.com/" + i + ".jpg");
            productService.createProduct(req);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("  [TIME] 200 product creations in " + elapsedMs + "ms");
        System.out.println("  [STAT] Throughput: " + String.format("%.0f", (200.0 * 1000) / elapsedMs) + " creates/sec");

        assertTrue(elapsedMs < 3000);
    }

    @Test
    @DisplayName("PERF-02: Large catalog fetch - 500 products in a single call")
    void largeCatalogFetch() {
        List<Product> bigCatalog = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            bigCatalog.add(Product.builder()
                    .id((long) i).name("Item-" + i)
                    .description("Desc-" + i).price(9.99 + i)
                    .imageUrl("https://img.com/" + i + ".jpg").build());
        }
        when(productRepository.findAll()).thenReturn(bigCatalog);

        long start = System.nanoTime();
        List<ProductResponseDTO> result = productService.getAllProducts();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertEquals(500, result.size());
        System.out.println("  [TIME] Fetched 500-product catalog in " + elapsedMs + "ms");
        assertTrue(elapsedMs < 500, "Large catalog fetch took " + elapsedMs + "ms");
    }

    @Test
    @DisplayName("PERF-03: 50 concurrent product reads")
    void concurrentProductReads() throws InterruptedException {
        Product product = Product.builder()
                .id(1L).name("Sneaker").description("Premium")
                .price(129.99).imageUrl("https://img.com/s.jpg").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        int concurrentUsers = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrentUsers);
        AtomicInteger success = new AtomicInteger(0);
        CopyOnWriteArrayList<Long> times = new CopyOnWriteArrayList<>();

        for (int i = 0; i < concurrentUsers; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    long t = System.nanoTime();
                    productService.getProductById(1L);
                    times.add((System.nanoTime() - t) / 1_000_000);
                    success.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        long start = System.nanoTime();
        startGate.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        long totalMs = (System.nanoTime() - start) / 1_000_000;

        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
        times.sort(Long::compareTo);
        long p95 = times.isEmpty() ? 0 : times.get((int) (times.size() * 0.95));

        System.out.println("\n  ==============================================");
        System.out.println("  CONCURRENT PRODUCT READ RESULTS");
        System.out.println("  ==============================================");
        System.out.println("  Concurrent users:  " + concurrentUsers);
        System.out.println("  Successful:        " + success.get());
        System.out.println("  Total time:        " + totalMs + "ms");
        System.out.println("  Avg response:      " + String.format("%.2f", avg) + "ms");
        System.out.println("  P95 response:      " + p95 + "ms");
        System.out.println("  Throughput:        " + String.format("%.0f", (success.get() * 1000.0) / Math.max(totalMs, 1)) + " reads/sec");
        System.out.println("  ==============================================\n");

        assertEquals(concurrentUsers, success.get());
    }

    @Test
    @DisplayName("PERF-04: CRUD cycle benchmark - create, read, update, delete x 100")
    void crudCycleBenchmark() {
        Product product = Product.builder()
                .id(1L).name("Test").description("Desc")
                .price(10.0).imageUrl("https://img.com/t.jpg").build();

        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsById(1L)).thenReturn(true);

        int cycles = 100;
        long start = System.nanoTime();

        for (int i = 0; i < cycles; i++) {
            // Create
            ProductRequestDTO req = new ProductRequestDTO();
            req.setName("P-" + i);
            req.setDescription("D-" + i);
            req.setPrice(10.0 + i);
            productService.createProduct(req);

            // Read
            productService.getProductById(1L);

            // Update
            productService.updateProduct(1L, req);

            // Delete
            productService.deleteProduct(1L);
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        int totalOps = cycles * 4;

        System.out.println("\n  ==============================================");
        System.out.println("  CRUD CYCLE BENCHMARK");
        System.out.println("  ==============================================");
        System.out.println("  Cycles:            " + cycles);
        System.out.println("  Total operations:  " + totalOps + " (C+R+U+D per cycle)");
        System.out.println("  Total time:        " + elapsedMs + "ms");
        System.out.println("  Operations/sec:    " + String.format("%.0f", (totalOps * 1000.0) / elapsedMs));
        System.out.println("  ==============================================\n");

        assertTrue(elapsedMs < 5000);
    }
}
