# Job Requirement to Technical Evidence Matrix

**Target Role:** Infosys — Specialist Programmer (Trainee)  
**Platform:** `event-driven-commerce-intelligence-platform`  
**Candidate:** Tejas H R | B.E. Information Science and Engineering (ISE), 2026 | CGPA: 8.01 / 10.0

---

## 1. Technical Coverage & Scope Mapping

| Job Requirement Area | Target Skill / Concept | Concrete Implementation in Platform | Implementation Status | Verification Method |
| :--- | :--- | :--- | :--- | :--- |
| **Java & JEE Architecture** | Modern Java LTS (21/25), modularity, concurrency | Java 21 Record types, Spring Boot 3.3.x, `@Transactional` boundaries, `CompletableFuture`, pessimistic & optimistic database locking | **VERIFIED LOCAL** | Unit & Concurrency test suites (`InventoryConcurrencyTest.java`) |
| **Spring Boot & Microservices** | Modular domain services, REST, Actuator, Security | Spring Boot application structured across domain modules (`catalog`, `inventory`, `order`, `payment`, `analytics`, `audit`), Spring Security 6 with JWT | **VERIFIED LOCAL** | Automated test suite (`OrderLifecycleIntegrationTest.java`) |
| **Event-Driven Architecture** | Kafka, asynchronous domain events, outbox, DLQ | Spring Kafka with Transactional Outbox Pattern, at-least-once delivery with consumer deduplication, Dead Letter Queue (DLQ) for poison pills | **VERIFIED LOCAL** | Consumer idempotency test (`KafkaOutboxConsumerTest.java`) |
| **Databases & Correctness** | PostgreSQL, relational modeling, NoSQL read models | PostgreSQL-compatible schema, ACID transactions on orders/inventory, Flyway database migrations, outbox table, read-model operational analytics | **VERIFIED LOCAL** | Transactional isolation & outbox tests |
| **Data Engineering & Analytics** | Real-time streams, functional pipelines, aggregations | Functional streaming analytics pipeline calculating gross merchandise value (GMV), stockout rates, and processing lag using pure Java Stream transforms | **VERIFIED LOCAL** | Analytics reconciliation test (`AnalyticsPipelineTest.java`) |
| **Frontend Engineering** | React, TypeScript, responsive UX, accessibility | React 18 + TypeScript + Tailwind CSS accessible SPA with real-time order tracking, catalog search, inventory gauges, and admin inspector | **VERIFIED LOCAL** | Production Vite build & Playwright E2E browser tests |
| **External API Provenance** | Public read-only catalog, fallback, caching | Real-time integration with FakeStore API (`https://fakestoreapi.com/products`) with explicit provenance labels: `LIVE_EXTERNAL_DATA`, `CACHED_EXTERNAL_DATA`, `SEEDED_DEMO_DATA` | **VERIFIED LIVE** | Live integration test & HTTP status probes |
| **DevOps & Containers** | Docker, multi-stage builds, CI/CD, health probes | Multi-stage `Dockerfile.backend`, `Dockerfile.frontend`, `docker-compose.yml` with health checks, GitHub Actions CI workflow | **VERIFIED LOCAL** | Docker compose specification & GitHub Actions workflow |
| **Software Craftsmanship** | Defect resolution, clean code, interview readiness | Documented engineering trade-offs (pessimistic vs optimistic locking, outbox polling vs CDC Debezium, exactly-once limits) | **VERIFIED LOCAL** | Architecture documentation & interview guide |

---

## 2. Technology Alternatives & Justification Matrix

| Technology | Role in Platform | Why Chosen Over Alternatives |
| :--- | :--- | :--- |
| **Spring Boot 3.3.x** | Core Application Framework | Standard enterprise backend framework offering native transaction management, dependency injection, and mature Kafka/JPA integration. |
| **Transactional Outbox** | Distributed Consistency | Guarantees atomic state updates and event emission within a single database transaction without requiring risky 2-Phase Commit (2PC / XA). |
| **Pessimistic Locking (`SELECT FOR UPDATE`)** | Inventory Race Condition Prevention | Defends against overselling when high concurrency targets low-stock items; guarantees serial execution at row level without optimistic retry storms. |
| **React 18 + TypeScript** | Client Application | Type-safe, accessible user interface with reactive state management, asynchronous polling/SSE updates, and clear loading/error boundaries. |
| **FakeStore API** | Live Catalog Data Provider | Verified public, permitted, read-only REST catalog providing real products, images, and categories with zero credential friction. |
