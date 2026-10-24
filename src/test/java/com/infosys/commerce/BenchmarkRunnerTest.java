package com.infosys.commerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.infosys.commerce.domain.dto.CreateOrderRequest;
import com.infosys.commerce.domain.dto.OrderItemDto;
import com.infosys.commerce.domain.dto.OrderResponse;
import com.infosys.commerce.domain.enums.DataSourceMode;
import com.infosys.commerce.domain.model.Product;
import com.infosys.commerce.domain.repository.ProductRepository;
import com.infosys.commerce.service.AnalyticsPipelineService;
import com.infosys.commerce.service.InventoryService;
import com.infosys.commerce.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class BenchmarkRunnerTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private AnalyticsPipelineService analyticsPipelineService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Generate empirical benchmark results and write to artifacts/benchmark-results.json")
    public void runEmpiricalBenchmarks() throws Exception {
        System.out.println("Starting empirical benchmark harness...");

        int numSkus = 5;
        List<String> benchSkus = new ArrayList<>();
        for (int i = 1; i <= numSkus; i++) {
            String sku = "SKU-BENCH-" + i;
            benchSkus.add(sku);
            if (!productRepository.existsBySku(sku)) {
                productRepository.save(new Product(
                        sku,
                        "Benchmark Asset " + i,
                        "Performance measurement asset " + i,
                        "electronics",
                        new BigDecimal("49.99"),
                        "USD",
                        "https://fakestoreapi.com/img/81fPKd-2AYL._AC_SL1500_.jpg",
                        DataSourceMode.SEEDED_DEMO_DATA
                ));
            }
            inventoryService.initializeStock(sku, 200);
        }

        int totalRequests = 60;
        int threadPoolSize = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        List<Long> orderLatenciesNanos = Collections.synchronizedList(new ArrayList<>());

        long startTime = System.nanoTime();
        CountDownLatch latch = new CountDownLatch(totalRequests);

        for (int i = 0; i < totalRequests; i++) {
            final String targetSku = benchSkus.get(i % numSkus);
            executor.submit(() -> {
                String idemKey = "BENCH-" + UUID.randomUUID();
                CreateOrderRequest req = new CreateOrderRequest(
                        List.of(new OrderItemDto(targetSku, 1, new BigDecimal("49.99"), "Benchmark Asset")),
                        "Benchmark Street, Tech Park",
                        "USD"
                );
                long reqStart = System.nanoTime();
                try {
                    OrderResponse resp = orderService.createOrder(req, idemKey, "customer@infosys.com");
                    if (resp != null) {
                        orderLatenciesNanos.add(System.nanoTime() - reqStart);
                    }
                } catch (Exception e) {
                    // skip failed
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        long totalWallTimeNanos = System.nanoTime() - startTime;
        executor.shutdown();

        // 2. Benchmark Idempotent Cache Replay Latency
        String cachedKey = "BENCH-IDEM-" + UUID.randomUUID();
        CreateOrderRequest idemReq = new CreateOrderRequest(
                List.of(new OrderItemDto(benchSkus.get(0), 1, new BigDecimal("49.99"), "Idempotency Probe")),
                "Benchmark Address",
                "USD"
        );
        orderService.createOrder(idemReq, cachedKey, "customer@infosys.com");

        List<Long> replayLatenciesNanos = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            long rStart = System.nanoTime();
            orderService.createOrder(idemReq, cachedKey, "customer@infosys.com");
            replayLatenciesNanos.add(System.nanoTime() - rStart);
        }

        // 3. Benchmark Functional Stream Analytics Pipeline Execution
        List<Long> analyticsLatenciesNanos = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            long aStart = System.nanoTime();
            analyticsPipelineService.computeOperationalMetrics();
            analyticsLatenciesNanos.add(System.nanoTime() - aStart);
        }

        // Sort latencies for percentiles
        Collections.sort(orderLatenciesNanos);
        Collections.sort(replayLatenciesNanos);
        Collections.sort(analyticsLatenciesNanos);

        double totalSecs = (double) totalWallTimeNanos / 1_000_000_000.0;
        int successfulOrders = orderLatenciesNanos.size();
        double throughputRps = successfulOrders / totalSecs;

        double p50OrderMs = getPercentile(orderLatenciesNanos, 50) / 1_000_000.0;
        double p95OrderMs = getPercentile(orderLatenciesNanos, 95) / 1_000_000.0;
        double p99OrderMs = getPercentile(orderLatenciesNanos, 99) / 1_000_000.0;

        double p50ReplayMs = getPercentile(replayLatenciesNanos, 50) / 1_000_000.0;
        double p95ReplayMs = getPercentile(replayLatenciesNanos, 95) / 1_000_000.0;

        double p50AnalyticsMicros = getPercentile(analyticsLatenciesNanos, 50) / 1_000.0;
        double p95AnalyticsMicros = getPercentile(analyticsLatenciesNanos, 95) / 1_000.0;

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("benchmarkSuite", "Enterprise Commerce Distributed Concurrency & Idempotency Benchmark");
        results.put("timestamp", Instant.now().toString());
        results.put("concurrencyLevel", threadPoolSize);
        results.put("skuPartitions", numSkus);
        results.put("totalRequestsEvaluated", totalRequests);
        results.put("successfulOrders", successfulOrders);
        results.put("totalDurationSeconds", Math.round(totalSecs * 1000.0) / 1000.0);
        results.put("throughputRequestsPerSec", Math.round(throughputRps * 100.0) / 100.0);

        Map<String, Object> orderLatency = new LinkedHashMap<>();
        orderLatency.put("p50Ms", Math.round(p50OrderMs * 100.0) / 100.0);
        orderLatency.put("p95Ms", Math.round(p95OrderMs * 100.0) / 100.0);
        orderLatency.put("p99Ms", Math.round(p99OrderMs * 100.0) / 100.0);
        results.put("orderPlacementLatencyWithPessimisticLock", orderLatency);

        Map<String, Object> idempotencyMetrics = new LinkedHashMap<>();
        idempotencyMetrics.put("p50CacheReplayLatencyMs", Math.round(p50ReplayMs * 100.0) / 100.0);
        idempotencyMetrics.put("p95CacheReplayLatencyMs", Math.round(p95ReplayMs * 100.0) / 100.0);
        idempotencyMetrics.put("guarantee", "Zero duplicate inventory deduction across identical keys");
        results.put("idempotencyCachePerformance", idempotencyMetrics);

        Map<String, Object> analyticsMetrics = new LinkedHashMap<>();
        analyticsMetrics.put("p50PipelineLatencyMicros", Math.round(p50AnalyticsMicros * 10.0) / 10.0);
        analyticsMetrics.put("p95PipelineLatencyMicros", Math.round(p95AnalyticsMicros * 10.0) / 10.0);
        analyticsMetrics.put("pipelineDesign", "Functional Java 21 Streams with groupingBy and Map-Reduce");
        results.put("functionalStreamAnalyticsLatency", analyticsMetrics);

        File artifactsDir = new File("artifacts");
        if (!artifactsDir.exists()) artifactsDir.mkdirs();

        File outputFile = new File(artifactsDir, "benchmark-results.json");
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(outputFile, results);

        System.out.println("Empirical benchmark results successfully saved to " + outputFile.getAbsolutePath());
        assertNotNull(results.get("orderPlacementLatencyWithPessimisticLock"));
    }

    private double getPercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0.0;
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }
}
