# Security & Data Governance Architecture

## 1. Authentication & Authorization Architecture

- **Stateless JWT Tokens:** Built with `io.jsonwebtoken (jjwt 0.12.6)` utilizing HMAC-SHA256 (`Keys.hmacShaKeyFor`) signing.
- **Spring Security 6 Filter Chain:** Configured with `SessionCreationPolicy.STATELESS` and custom `JwtAuthenticationFilter` executing before `UsernamePasswordAuthenticationFilter`.
- **Role-Based Access Control (RBAC):**
  - `CUSTOMER`: Can place orders, inspect catalog, view personal order history, and cancel own active orders.
  - `INVENTORY_MANAGER`: Can inspect inventory levels, restock SKUs, view operational order velocity.
  - `ADMIN`: Full administrative control across analytics, system adapters, outbox queues, and DLQ remediation.

---

## 2. Concurrency & Transactional Data Integrity

- **Pessimistic Locking (`LockModeType.PESSIMISTIC_WRITE`):**
  Guarantees row-level isolation on the `inventory` table (`SELECT FOR UPDATE`). Concurrent order submissions targeting the same inventory row block deterministically until the active transaction commits or rolls back, preventing negative inventory and overselling.
- **Transactional Outbox Dual-Write Safety:**
  State changes to orders and event generation are committed atomically within the same database transaction. System crashes between database persistence and message broker delivery will never drop events.
- **Consumer Idempotency Deduplication:**
  All event consumers verify the incoming event key against the `processed_events` table before applying business logic, protecting against at-least-once message duplication.

---

## 3. Web & Transport Security

- **Strict CORS Policy:** Configured via `CorsConfigurationSource` with exposed `Idempotency-Key` and `X-Correlation-ID` headers.
- **SQL Injection Prevention:** 100% of queries use Spring Data JPA repository methods and parameterized JPQL queries with named parameters (`@Param`).
- **Defensive Error Handling:** Custom `GlobalExceptionHandler` intercepts exceptions and returns sanitized JSON error payloads, preventing stack trace or internal database schema leakage to API consumers.
