# Local Setup and Execution Guide

This document describes how to build, test, and run the **Event-Driven Commerce Intelligence Platform** locally.

---

## 1. Prerequisites

- **Java Development Kit:** JDK 21+ (Tested on OpenJDK Temurin-25.0.4+7-LTS, Java 21 bytecode target)
- **Node.js & npm:** Node.js 18+ (Tested on Node.js v26.12.1 and npm 11.20.0)
- **Build Tool:** Apache Maven 3.9+ (Embedded `./mvnw` wrapper included in repository)

---

## 2. Quickstart (Full-Stack Monolith Mode)

The React 18 TypeScript frontend is compiled into Spring Boot's static resource directory (`src/main/resources/static`). A single executable jar serves both the REST API and the frontend UI.

```bash
# 1. Build the React frontend
cd frontend
npm install
npm run build
cd ..

# 2. Compile and package Spring Boot
./mvnw clean package -DskipTests

# 3. Launch the platform
java -jar target/event-driven-commerce-intelligence-platform-1.0.0.jar
```

Access the application in your browser:
- **Interactive Full-Stack Web UI:** `http://localhost:8080/`
- **H2 In-Memory Database Console:** `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:commercedb`
  - User: `sa`
  - Password: *(blank)*
- **Actuator Health & Metrics:** `http://localhost:8080/actuator/health`

---

## 3. Running Automated Tests & Benchmarks

```bash
# Run all 15 automated JUnit 5 tests
./mvnw test

# Run empirical benchmark suite generating artifacts/benchmark-results.json
./mvnw test -Dtest=BenchmarkRunnerTest
```

---

## 4. Development Mode (Hot Reload)

For active full-stack development with hot-module replacement (HMR):

### Terminal 1: Backend
```bash
./mvnw spring-boot:run
```

### Terminal 2: Frontend Vite Dev Server
```bash
cd frontend
npm run dev
```
Vite runs on `http://localhost:3000` and automatically proxies all `/api` requests to the Spring Boot backend on port 8080.

---

## 5. Docker Deployment

To launch backend and database via Docker:

```bash
docker compose up --build
```
