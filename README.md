<div align="center">

# 🛡️ SentinelGate

**Production-Grade Secure API Gateway & Real-Time Security Analytics Platform**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java: 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot: 3.3.4](https://img.shields.io/badge/Spring_Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud Gateway](https://img.shields.io/badge/Spring_Cloud-Gateway_2023.0.3-green.svg)](https://spring.io/projects/spring-cloud-gateway)
[![Redis: 7](https://img.shields.io/badge/Redis-7.0-red.svg)](https://redis.io/)
[![PostgreSQL: 16](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![React: 18](https://img.shields.io/badge/React-18.3-61dafb.svg)](https://react.dev/)
[![Tests: 49/49 Passing](https://img.shields.io/badge/Tests-49%2F49%20Passing-brightgreen.svg)](backend/src/test)
[![Build Status](https://img.shields.io/badge/CI-Passing-success.svg)](.github/workflows/ci.yml)

<p align="center">
  <b>SentinelGate</b> is an enterprise-grade, security-focused API Gateway and Security Operations Center (SOC) dashboard positioned between public clients and backend microservices. Built with high-throughput reactive Java 21, Spring Cloud Gateway, Redis 7 sliding-window counters, PostgreSQL persistence, Prometheus metrics scraping, and a React 18 dark-mode analytics console.
</p>

</div>

---

## 📑 Table of Contents

- [Architectural Overview](#-architectural-overview)
- [Key Engineering Pillars](#-key-engineering-pillars)
- [Features & Capabilities](#-features--capabilities)
- [System Architecture Diagram](#-system-architecture-diagram)
- [Technology Stack](#-technology-stack)
- [Quick Start with Docker](#-quick-start-with-docker)
- [Local Development](#-local-development)
- [API Reference](#-api-reference)
- [Threat Model & Defensive Matrix](#-threat-model--defensive-matrix)
- [Automated Test Suite](#-automated-test-suite)
- [Observability (Prometheus & Grafana)](#-observability-prometheus--grafana)
- [Security Policy](#-security-policy)
- [License](#-license)

---

## 🏛️ Architectural Overview

SentinelGate operates as the single point of entry for all incoming traffic. Requests flow through an ordered chain of non-blocking reactive filters that perform authentication, server-side authorization, machine API key hashing, rate limiting, and threat telemetry tracking before dynamically proxying traffic to backend target microservices.

```
                              REQUEST PROCESSING PIPELINE
┌───────────────────────────────────────────────────────────────────────────────────────┐
│  Client (Browser / SPA / Mobile / Machine Agent)                                      │
└──────────────────────────────────────────┬────────────────────────────────────────────┘
                                           │ HTTP / HTTPS
                                           ▼
┌───────────────────────────────────────────────────────────────────────────────────────┐
│  SENTINELGATE API GATEWAY (Spring Boot 3.3 + Spring Cloud Gateway + WebFlux / Netty) │
│                                                                                       │
│   1. [ JwtSecurityContextFilter ]  ──► WebFilter: populates ReactiveSecurityContext   │
│   2. [ Spring Security Filter ]    ──► Enforces server-side .hasAuthority("ADMIN")   │
│   3. [ ApiKeyValidationFilter ]    ──► WebFilter: checks format, DB hash, revocation  │
│   4. [ RateLimiterFilter ]         ──► WebFilter: Redis sliding-window limit (429)    │
│   5. [ JwtAuthenticationFilter ]   ──► GlobalFilter: enriches X-User-* downstream     │
│   6. [ LoggingAuditFilter ]        ──► GlobalFilter: Micrometer metrics & audit logs  │
│   7. [ DynamicRouteLocator ]       ──► Resolves dynamic routes from PostgreSQL DB     │
└──────────────────────┬───────────────────────────┬─────────────────────┬──────────────┘
                       │                           │                     │
                       ▼                           ▼                     ▼
              ┌─────────────────┐         ┌─────────────────┐    ┌──────────────┐
              │   PostgreSQL    │         │     Redis 7     │    │  Downstream  │
              │ (Users, Routes, │         │ (Sliding Window │    │ Microservice │
              │ Events, Audits) │         │ Counters, TTL)  │    │   Targets    │
              └─────────────────┘         └─────────────────┘    └──────────────┘
```

---

## ⚡ Key Engineering Pillars

1. **Fully Reactive & Non-Blocking**: Built entirely on Spring WebFlux and Project Reactor. All blocking database interactions (Spring Data JPA) are explicitly offloaded to `Schedulers.boundedElastic()` to ensure Netty's event-loop threads never starve.
2. **Server-Side Authorization (RBAC)**: Security is never enforced on the client alone. The gateway validates HMAC-SHA512 JWT claims and populates the Spring Security context prior to route authorization evaluation.
3. **Multi-Subject Redis Rate Limiting**: Enforces sliding-window rate limits scoped by subject identity (`ip:<ip>`, `user:<username>`, `apikey:<prefix>`, `auth-ip:<ip>`) with atomic increments, TTL expiry, and standard `Retry-After` headers.
4. **Machine API Key Lifecycle**: Generates cryptographically secure `sg_live_<8-hex-prefix>_<24-hex-secret>` tokens. Keys are stored as irreversible BCrypt hashes and can be revoked instantly via prefix lookup.
5. **Zero-Fake-Data Telemetry**: Analytics endpoints calculate metrics directly from live PostgreSQL database records and provide 12 five-minute time-series buckets without artificial metric padding.

---

## 🚀 Features & Capabilities

| Module | Technical Implementation | Description |
| :--- | :--- | :--- |
| **Authentication** | HMAC-SHA512 JWT | Stateless user authentication with role-based claims (`ADMIN`, `DEVELOPER`, `VIEWER`) and token introspection via `/api/v1/auth/me`. |
| **Machine API Keys** | BCrypt Hashed Storage | Prefixed `sg_live_` keys for machine-to-machine clients with one-time raw secret display, instant revocation, and expiration checks. |
| **Adaptive Rate Limiting** | Redis Sliding Windows | High-throughput distributed rate limiting with custom quotas (Auth: 20 req/min, Anonymous IP: 100 req/min, User: 300 req/min, API Key: 1000 req/min). |
| **Threat Detection Engine** | Redis + PostgreSQL | Rule-based engine detecting brute-force login attempts (5 failures in 300s), rate-limit breaches, and unauthorized access patterns. |
| **Dynamic Routing** | Database-Backed RouteLocator | Hot-reconfigurable gateway routes mapped to target backend services with per-route rate limits and role restrictions. |
| **Real-Time SOC Dashboard** | React 18 + Recharts | Dark-theme administrative dashboard displaying live request timelines, threat breakdowns, route controls, and API key management. |
| **Structured Audit Logs** | Administrative Audit Trail | Immutable audit logging for all route registrations, key creations, policy modifications, and revocations. |
| **Observability Stack** | Prometheus + Grafana | Native Micrometer metrics endpoint (`/actuator/prometheus`) and pre-provisioned Grafana visualization dashboards. |

---

## 🛠️ Technology Stack

### Backend
- **Java 21 LTS** (Modern language features, pattern matching, records)
- **Spring Boot 3.3.4** & **Spring Cloud Gateway 2023.0.3**
- **Spring WebFlux & Project Reactor** (High-throughput reactive I/O)
- **Spring Security 6** (Reactive JWT & RBAC filters)
- **Spring Data JPA & Hibernate 6**
- **PostgreSQL 16** & **Redis 7** (Lettuce reactive driver)
- **JJWT (io.jsonwebtoken: 0.12.6)** (HMAC-SHA512 token signing)
- **Micrometer & Prometheus Registry** (Actuator metrics)

### Frontend
- **React 18.3** & **TypeScript 5.5**
- **Vite 5.4** (Fast production bundling)
- **Tailwind CSS 3.4** (Custom cyberpunk dark theme)
- **Recharts 2.12** (Interactive telemetry time-series charts)
- **Lucide React** (Modern iconography)
- **Axios** (With automated JWT bearer request interceptors)

### DevOps & Infrastructure
- **Docker & Docker Compose** (6-service container orchestration)
- **Nginx** (High-performance SPA reverse proxy)
- **Prometheus 2.51** & **Grafana 10.4** (Pre-provisioned datasources & dashboards)
- **GitHub Actions** (Automated CI test, typecheck, and build pipeline)

---

## 🐳 Quick Start with Docker

Launch the complete 6-service SentinelGate stack in one command:

```bash
# Clone the repository
git clone https://github.com/VARDHAN2254/SentinelGate.git
cd SentinelGate/docker

# Build and start all services
docker compose up --build
```

### Service Map

| Service | Address | Default Credentials |
| :--- | :--- | :--- |
| **SentinelGate Dashboard** | [http://localhost:80](http://localhost:80) | `admin` / `AdminSecret123!` |
| **API Gateway Core** | [http://localhost:8080](http://localhost:8080) | — |
| **Grafana Monitoring** | [http://localhost:3000](http://localhost:3000) | `admin` / `admin` |
| **Prometheus Metrics** | [http://localhost:9090](http://localhost:9090) | — |
| **PostgreSQL Database** | `localhost:5432` | `sentinelgate_user` / `sentinelgate_password` |
| **Redis Cache** | `localhost:6379` | — |

---

## 💻 Local Development

### 1. Start Infrastructure Dependencies
```bash
cd docker
docker compose up sentinelgate-postgres sentinelgate-redis prometheus grafana -d
```

### 2. Run Backend
```bash
cd backend
mvn spring-boot:run
```
*(Or run standalone with H2 database: `mvn spring-boot:run -Dspring-boot.run.profiles=local`)*

### 3. Run Frontend
```bash
cd frontend
npm install
npm run dev
```
Navigate to `http://localhost:5173`.

---

## 📖 API Reference

### 🔐 Authentication Endpoints

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "developer1",
  "email": "dev@sentinelgate.io",
  "password": "SecurePassword123!",
  "role": "DEVELOPER"
}
```

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "AdminSecret123!"
}
```

```http
GET /api/v1/auth/me
Authorization: Bearer <JWT_TOKEN>
```

---

### 📊 Analytics & Telemetry

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/analytics/overview` | Returns live counts for security events, auth failures, rate limit hits, and active routes. |
| `GET` | `/api/v1/analytics/traffic-timeline` | Returns 12 × 5-minute time-series buckets populated from database events. |
| `GET` | `/api/v1/analytics/events?page=0&size=20&severity=HIGH` | Paginated security threat events with filtering by severity and event type. |

---

### ⚙️ Administration & Routing (Requires `ADMIN` Role)

```http
# Create a new machine API key
POST /api/v1/admin/api-keys
Authorization: Bearer <ADMIN_JWT>
Content-Type: application/json

{
  "name": "Payment Microservice Client",
  "rateLimitPerMin": 500
}
```

```http
# Revoke an API key
POST /api/v1/admin/api-keys/{id}/revoke
Authorization: Bearer <ADMIN_JWT>
```

```http
# Register a dynamic gateway route
POST /api/v1/admin/routes
Authorization: Bearer <ADMIN_JWT>
Content-Type: application/json

{
  "routeId": "payment_service_route",
  "serviceId": 1,
  "pathPattern": "/api/v1/payments/**",
  "requiresAuth": true,
  "allowedRoles": "ADMIN,DEVELOPER",
  "rateLimitPerMin": 300,
  "isActive": true
}
```

---

## 🛡️ Threat Model & Defensive Matrix

| Threat Vector | Gateway Detection Mechanism | Enforcement Action |
| :--- | :--- | :--- |
| **Credential Stuffing** | Dedicated per-IP rate limiting on `/api/v1/auth/**` (20 req/min) | `HTTP 429 Too Many Requests` + Audit Log |
| **Brute-Force Login** | Redis failure counter per `ip:username` pair (5 failures / 300s window) | `BRUTE_FORCE` Security Event + `BLOCK_TEMPORARY` |
| **API Flooding / DoS** | Per-subject Redis sliding-window counters (IP, User, Key) | `HTTP 429` with `Retry-After` response header |
| **Revoked API Key Reuse** | Real-time database status validation on `X-API-KEY` header | `HTTP 403 Forbidden` |
| **Expired API Key Reuse** | Timestamp validation against `expires_at` attribute | `HTTP 403 Forbidden` |
| **Malformed Key Attack** | Format verification (`sg_live_<prefix>_<secret>`) | `HTTP 401 Unauthorized` |
| **Forged / Expired JWT** | Cryptographic HMAC-SHA512 signature and claims verification | `HTTP 401 Unauthorized` |
| **Privilege Escalation** | Server-side Spring Security `.hasAuthority("ADMIN")` evaluation | `HTTP 403 Forbidden` |

---

## 🧪 Automated Test Suite

SentinelGate features a 100% passing automated test suite covering unit, reactive service, filter, and full end-to-end integration scenarios:

```bash
cd backend
mvn test
```

### Test Coverage Breakdown (49 Tests Across 9 Classes)

```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
[INFO] Running com.sentinelgate.integration.GatewaySecurityIntegrationTest
  - healthCheck_returnsUp                                            [PASS]
  - register_validUser_returns201                                    [PASS]
  - register_duplicateUsername_returns409                            [PASS]
  - login_validCredentials_returnsJwt                                [PASS]
  - login_wrongPassword_returns401                                   [PASS]
  - login_nonExistentUser_returns401                                 [PASS]
  - me_invalidJwt_returns401                                         [PASS]
  - me_noToken_returns401                                            [PASS]
  - me_validAdminToken_returnsProfile                                [PASS]
  - adminEndpoint_noToken_returns401                                 [PASS]
  - adminEndpoint_withViewerToken_returns403                         [PASS]
  - adminEndpoint_withAdminToken_returns200                          [PASS]
  - bruteForce_5FailedLogins_generatesAuthFailureEvents              [PASS]
  - apiKey_create_returnsRawKey                                      [PASS]
  - apiKey_fullLifecycle_createRevokeReject                          [PASS]
  - apiKey_malformed_returns401                                      [PASS]
  - apiKey_unknown_returns401                                        [PASS]
  - apiKey_expired_returns403                                        [PASS]
  - analytics_overview_returnsRealCounts                             [PASS]
  - analytics_timeline_returns12Buckets                              [PASS]
  - analytics_events_returnsPaginatedResults                         [PASS]
  - analytics_events_filterBySeverity                                [PASS]
  - analytics_events_filterByEventType                               [PASS]
  - auditLogs_adminCanView                                           [PASS]
  - securityRules_adminCanView                                       [PASS]
  - gatewayRoutes_adminCanView                                       [PASS]
[INFO] Running com.sentinelgate.integration.RateLimitE2EIntegrationTest
  - rateLimit_burstOf6_triggers429On6th                              [PASS]
  - rateLimit_identityIsolation_userBNotBlocked                      [PASS]
  - rateLimit_windowReset_allowedAgain                               [PASS]
[INFO] Running com.sentinelgate.security.JwtTokenProviderTest        [4 PASS]
[INFO] Running com.sentinelgate.service.AuthServiceTest              [4 PASS]
[INFO] Running com.sentinelgate.service.ApiKeyServiceTest            [2 PASS]
[INFO] Running com.sentinelgate.service.RateLimitingServiceTest      [3 PASS]
[INFO] Running com.sentinelgate.service.SecurityDetectionEngineTest [2 PASS]
[INFO] Running com.sentinelgate.service.AuditLogServiceTest          [2 PASS]
[INFO] Running com.sentinelgate.service.GatewayRouteServiceTest      [2 PASS]
[INFO] Running com.sentinelgate.SentinelGateApplicationTests         [1 PASS]

Results: 49 Tests run, 0 Failures, 0 Errors, 0 Skipped
```

---

## 📈 Observability (Prometheus & Grafana)

SentinelGate instruments metrics natively using **Micrometer**:

- `sentinelgate_security_threats_total`: Counter partitioned by `event_type` and `severity`.
- `sentinelgate_ratelimit_violations_total`: Counter tracking rate limit violations by subject (`ip`, `user`, `apikey`).
- Standard JVM, garbage collection, and Netty connection thread-pool metrics.

Scrape configuration is provisioned in `docker/prometheus/prometheus.yml` and visual dashboards are pre-loaded via `docker/grafana/provisioning/`.

---

## 🔒 Security Policy

Please review [SECURITY.md](SECURITY.md) for vulnerability reporting guidelines and responsible disclosure procedures. Do not submit sensitive vulnerabilities through public issue trackers.

---

## 📄 License

This project is licensed under the terms of the **MIT License**.

See the full license text in [LICENSE](LICENSE).

```
MIT License

Copyright (c) 2026 VARDHAN2254

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

<div align="center">
  <b>Built with ❤️ by <a href="https://github.com/VARDHAN2254">VARDHAN2254</a></b>
</div>
