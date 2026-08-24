# SentinelGate

**Secure API Gateway & Security Analytics Platform**

SentinelGate is a production-quality portfolio project demonstrating the engineering disciplines involved in building a security-focused API gateway: backend architecture, reactive programming, authentication and authorisation, rate limiting, threat detection, observability, containerisation, and CI/CD.

---

## Architecture

```
Client Request
     │
     ▼
JwtSecurityContextFilter (WebFilter — populates Spring Security context)
     │
     ▼
Spring Security (evaluates hasAuthority rules)
     │
     ▼
JwtAuthenticationFilter (GlobalFilter — enriches proxied request headers)
     │
ApiKeyValidationFilter  (GlobalFilter — validates machine clients)
     │
RateLimiterFilter       (GlobalFilter — per-subject sliding-window limits)
     │
LoggingAuditFilter      (GlobalFilter — Micrometer counters, structured logs)
     │
     ▼
Backend Routes (registered in DB → DynamicRouteLocator → Spring Cloud Gateway)
     │
     ▼
Downstream Microservice
```

The gateway is built on **Spring Boot 3 + Spring Cloud Gateway + WebFlux** (Netty). All security processing happens in the reactive filter chain. Routes are stored in PostgreSQL and resolved dynamically at request time. Rate limiting uses Redis sliding windows.

---

## Features

| Area | What it does |
|------|-------------|
| JWT Authentication | RS-256 access tokens + role claims; `/me` endpoint for token introspection |
| API Key Validation | `sg_live_` prefixed keys; BCrypt-hashed storage; revocable; per-key rate limits |
| Rate Limiting | Redis sliding-window; per-IP, per-user, per-API-key; auth endpoints capped at 20 req/min; HTTP 429 with Retry-After |
| Brute-Force Detection | Redis counter per `ip:user` pair; BRUTE_FORCE event at 5 failures within 300 s |
| Security Events | All threats persisted to PostgreSQL; queryable by type, severity, time range |
| Gateway Routing | Routes registered in DB with path pattern, auth requirements, allowed roles, rate limits |
| Analytics API | Real-time metrics from DB (zero hard-coded values); 5-min traffic-timeline buckets |
| Observability | Micrometer counters → Prometheus → Grafana (auto-provisioned datasource) |
| Audit Log | Structured audit trail for all administrative actions |
| Demo Data | Realistic seed of 12 security events on first startup (labelled `[DEMO]`) |

---

## Quick Start

**Prerequisites:** Docker + Docker Compose

```bash
# Start all six services: backend, frontend, postgres, redis, prometheus, grafana
cd docker
docker compose up --build
```

| Service | URL |
|---------|-----|
| SentinelGate Dashboard | http://localhost:80 |
| API Gateway | http://localhost:8080 |
| Grafana | http://localhost:3000 (admin / admin) |
| Prometheus | http://localhost:9090 |

Default admin credentials: `admin` / `AdminSecret123!`  
Override via environment variables: `ADMIN_USERNAME`, `ADMIN_PASSWORD`

---

## Local Development

```bash
# Start only infrastructure
cd docker && docker compose up sentinelgate-postgres sentinelgate-redis prometheus grafana -d

# Start backend
cd backend && mvn spring-boot:run

# Start frontend
cd frontend && npm install && npm run dev
```

---

## API Reference

### Auth

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/v1/auth/register` | None |
| POST | `/api/v1/auth/login` | None |
| GET | `/api/v1/auth/me` | Bearer JWT |

### Analytics

| Method | Path | Returns |
|--------|------|---------|
| GET | `/api/v1/analytics/overview` | Live security metrics from DB |
| GET | `/api/v1/analytics/traffic-timeline` | 12 × 5-min buckets for the last hour |
| GET | `/api/v1/analytics/events?page=0&size=20&severity=HIGH&eventType=AUTH_FAILURE` | Paginated security events |

### Admin (requires `ADMIN` role)

| Method | Path |
|--------|------|
| GET/POST | `/api/v1/admin/routes` |
| GET/POST | `/api/v1/admin/services` |
| GET/POST | `/api/v1/admin/api-keys` |
| POST | `/api/v1/admin/api-keys/{id}/revoke` |
| GET | `/api/v1/admin/security-rules` |

---

## Rate Limiting

| Client type | Limit | Window | Key |
|-------------|-------|--------|-----|
| API key | 1,000 req | 60 s | `apikey:<prefix>` |
| Authenticated user | 300 req | 60 s | `user:<username>` |
| Anonymous IP | 100 req | 60 s | `ip:<addr>` |
| Any IP (auth endpoints) | 20 req | 60 s | `auth-ip:<addr>` |

Rate limit responses include `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, and `Retry-After` headers.

---

## Threat Model

| Threat | Detection | Response |
|--------|-----------|----------|
| Credential stuffing | Auth endpoint rate limit: 20 req/min per IP | HTTP 429 + security event |
| Brute-force login | 5 failures from same IP within 300 s | BRUTE_FORCE event + BLOCK_TEMPORARY action |
| API abuse / scraping | Per-subject rate limit on all routes | HTTP 429 with Retry-After |
| Revoked API key reuse | Status check on every request | HTTP 403 FORBIDDEN |
| Expired API key reuse | Expiry check on every request | HTTP 403 FORBIDDEN |
| Malformed API key | Format + prefix lookup | HTTP 401 UNAUTHORIZED |
| Invalid JWT | Token validation on every protected request | HTTP 401 UNAUTHORIZED |
| Role escalation | Spring Security `hasAuthority()` enforced server-side | HTTP 403 FORBIDDEN |

---

## Test Coverage

```bash
cd backend && mvn test
```

**49 tests across 9 test classes:**

| Class | Count | Covers |
|-------|-------|--------|
| `GatewaySecurityIntegrationTest` | 26 | Auth, RBAC (403), brute-force detection, API key lifecycle (create/use/revoke/expired/malformed/unknown), analytics endpoints & filters, audit trail, security rules, gateway routes |
| `RateLimitE2EIntegrationTest` | 3 | Rate-limit burst to 429, Retry-After header, Redis counter & TTL, SecurityEvent creation, identity isolation, sliding window reset |
| `AuthServiceTest` | 4 | Registration, login, duplicate handling |
| `ApiKeyServiceTest` | 2 | Key generation, revocation |
| `RateLimitingServiceTest` | 3 | Allow, deny, window expiry |
| `SecurityDetectionEngineServiceTest` | 2 | Auth failure + brute-force event generation |
| `AuditLogServiceTest` | 2 | Structured audit trail |
| `GatewayRouteServiceTest` | 2 | Route registration, lookup |
| `JwtTokenProviderTest` | 4 | Token generation, claims parsing, signature validation, expiration |
| `SentinelGateApplicationTests` | 1 | Context loads |

---

## Known Limitations

- **Brute-force threshold in tests**: Redis is unavailable in the test profile, so the brute-force counter can't accumulate. Integration tests verify `AUTH_FAILURE` events only; BRUTE_FORCE events are verified at the service layer in `SecurityDetectionEngineServiceTest`.
- **Traffic timeline**: The chart shows security events (auth failures, rate limit hits, brute-force detections), not total HTTP request volume. A full request log would require a dedicated access-log table or a Prometheus counter query.
- **Demo data**: The first startup seeds 12 demo security events (tagged `[DEMO]`) so the dashboard is meaningful on first run. Disable with `sentinelgate.demo.seed-events=false`.
- **CORS**: `allowedOriginPatterns("*")` is used for local development. Set specific origins in production.

---

## Project Structure

```
SentinelGate/
├── backend/                    Spring Boot 3 application
│   ├── src/main/java/com/sentinelgate/
│   │   ├── config/             SecurityConfig, DataInitializer
│   │   ├── domain/             JPA entities
│   │   ├── dto/                Request/response DTOs
│   │   ├── gateway/filter/     GlobalFilters (JWT, ApiKey, RateLimit, Logging)
│   │   ├── repository/         Spring Data JPA repositories
│   │   ├── security/           JwtTokenProvider, JwtSecurityContextFilter (WebFilter)
│   │   ├── service/            Business logic
│   │   └── web/                REST controllers
│   └── src/test/               Integration + unit tests
├── frontend/                   React + TypeScript + Recharts dashboard
├── docker/                     Docker Compose, Prometheus, Grafana provisioning
└── .github/workflows/ci.yml    GitHub Actions (test + build)
```
