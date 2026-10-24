# System Limitations & Production Hardening Roadmap

This document outlines architectural tradeoffs, current operational constraints, and the evolutionary path from local evaluation to large-scale enterprise deployment.

---

## 1. Storage & Persistence Tradeoffs

- **Current Implementation:** H2 in-memory database with PostgreSQL compatibility mode (`MODE=PostgreSQL`) and write-ahead transaction semantics.
- **Limitation:** In-memory persistence resets between process restarts. While optimal for reproducible local verification and CI/CD pipelines, production requires a clustered database.
- **Production Hardening:**
  - Transition to managed PostgreSQL (e.g. Amazon RDS / Azure Database for PostgreSQL) with read replicas.
  - Apply Flyway or Liquibase for versioned schema migrations instead of `hibernate.ddl-auto=update`.

---

## 2. Event Streaming & Message Broker

- **Current Implementation:** Transactional Outbox pattern with in-JVM asynchronous polling dispatcher (`@Scheduled(fixedDelay = 1000)`).
- **Limitation:** In multi-node deployments, competing polling workers require distributed locking (e.g. ShedLock or Debezium CDC) to avoid redundant polling queries.
- **Production Hardening:**
  - Introduce Apache Kafka cluster with partitioned topics (`order.events`, `inventory.events`).
  - Deploy Debezium CDC (Change Data Capture) connector reading PostgreSQL WAL logs directly, eliminating polling overhead and reducing outbox latency to sub-millisecond ranges.

---

## 3. External API Catalog Dependencies

- **Current Implementation:** Public FakeStore API (`https://fakestoreapi.com/products`) with 3-second connect/read timeout and automatic cached fallback catalog.
- **Limitation:** Public third-party APIs may experience rate limiting, network latency, or intermittent outages outside local control.
- **Production Hardening:**
  - Implement Redis distributed caching with TTL for catalog responses.
  - Integrate Resilience4j Circuit Breaker with sliding window failure rates.
