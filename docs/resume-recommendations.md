# Verified Résumé Recommendations & Claim-to-Evidence Matrix

**Target Role:** Infosys — Specialist Programmer (Trainee)  
**Candidate:** Tejas H R  
**Verified Platform:** `event-driven-commerce-intelligence-platform`  

---

## 1. Verified XYZ Résumé Bullets

### Bullet 1: Concurrency Control & High-Throughput Order Reservation
> **Accomplished** zero-overselling inventory reservation under race conditions with **453.01 req/sec peak throughput** and **6.70 ms P50 latency (67.02 ms P95)**, **by engineering** row-level pessimistic locking (`LockModeType.PESSIMISTIC_WRITE`) and atomic stock transitions across 20 concurrent threads in Spring Boot 3.3 and Java 21.

### Bullet 2: Transactional Outbox Pattern & Consumer Deduplication
> **Accomplished** 100% dual-write reliability and **0.69 ms P50 idempotency replay latency** without duplicate inventory deductions, **by implementing** the Transactional Outbox pattern, client UUID `Idempotency-Key` caching, and dead-letter queue (DLQ) routing verified across a **15-test passing JUnit 5 suite (100% pass rate)**.

### Bullet 3: Functional Stream Analytics & Full-Stack Interface
> **Accomplished** real-time merchandise valuation and category demand aggregation at **4.34 ms P50 pipeline execution**, **by authoring** non-blocking Java 21 Stream map-reduce pipelines integrated with a React 18 TypeScript dashboard and public REST catalog synchronization.

---

## 2. Claim-to-Evidence Table

| Resume Claim | Exact Metric | Source Artifact & Code Reference | Verification Method |
| :--- | :--- | :--- | :--- |
| **High-concurrency order placement throughput** | **453.01 req/sec** | [`artifacts/benchmark-results.json`](../artifacts/benchmark-results.json) | Measured over 60 concurrent requests across 5 SKU partitions |
| **Pessimistic locking reservation latency** | **P50: 6.70 ms, P95: 67.02 ms, P99: 71.52 ms** | [`artifacts/benchmark-results.json`](../artifacts/benchmark-results.json) | Benchmark harness executing `orderService.createOrder` |
| **Idempotency replay cache hit latency** | **P50: 0.69 ms, P95: 1.09 ms** | [`artifacts/benchmark-results.json`](../artifacts/benchmark-results.json) | 50 consecutive replay evaluations of identical `Idempotency-Key` |
| **Zero overselling under race conditions** | **0 overselling** (5 succeeded, 15 rejected) | [`src/test/java/com/infosys/commerce/InventoryConcurrencyTest.java`](../src/test/java/com/infosys/commerce/InventoryConcurrencyTest.java) | 20 threads unleashed simultaneously via `CountDownLatch` on 5 items |
| **Stream analytics execution latency** | **P50: 4.34 ms (4,344.4 µs)** | [`artifacts/benchmark-results.json`](../artifacts/benchmark-results.json) | 50 functional stream pipeline evaluations |
| **Automated test suite pass rate** | **15 passed, 0 failed (100% pass rate)** | [`artifacts/test-summary.json`](../artifacts/test-summary.json) | Maven Surefire execution across all test classes |

---

## 3. Forbidden Claims Guide

| Forbidden / Misleading Claim | Reason Forbidden | Permissible Honest Phrasing |
| :--- | :--- | :--- |
| "Engineered multi-node Apache Kafka and Apache Spark cluster in production" | Platform runs embedded event bus with Kafka-compatible outbox design locally; full cluster requires enterprise infrastructure. | "Architected event-driven outbox and consumer deduplication pipeline with DLQ routing ready for Kafka topics." |
| "Trained neural deep-learning recommendation engine" | Platform uses functional Java Stream analytics for demand profiling, not deep learning neural networks. | "Engineered pure functional Java 21 Stream aggregation pipelines computing GMV and category demand in 4.34 ms." |
| "Handled millions of production e-commerce users" | Application was validated under local concurrency stress tests, not live production Internet traffic. | "Benchmarked high-concurrency order submission at 453.01 req/sec with zero overselling across 20 concurrent threads." |
