package com.sentinelgate.integration;

import com.sentinelgate.domain.enums.EventType;
import com.sentinelgate.domain.enums.SeverityLevel;
import com.sentinelgate.repository.SecurityEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration test certifying the complete rate-limiting subsystem:
 * 1. 5 allowed requests from the same identity
 * 2. 6th request triggers HTTP 429 TOO_MANY_REQUESTS
 * 3. Verified JSON response body and Retry-After header
 * 4. Verified Redis key increment and TTL assignment
 * 5. Verified SecurityEvent persistence in PostgreSQL/H2 and Analytics overview
 * 6. Verified Identity Isolation (Identity B is allowed while Identity A is blocked)
 * 7. Verified window reset behavior
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:ratelimitdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "sentinelgate.demo.seed-events=false",
                // Configure low test thresholds
                "sentinelgate.ratelimit.ip-limit=5",
                "sentinelgate.ratelimit.auth-limit=5",
                "sentinelgate.ratelimit.user-limit=5",
                "sentinelgate.ratelimit.api-key-limit=5"
        }
)
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class RateLimitE2EIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private SecurityEventRepository securityEventRepository;

    private static final ConcurrentHashMap<String, AtomicLong> redisStore = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> redisTtl = new ConcurrentHashMap<>();

    @TestConfiguration
    static class TestRedisConfig {
        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        public ReactiveStringRedisTemplate reactiveStringRedisTemplate() {
            ReactiveStringRedisTemplate mockTemplate = Mockito.mock(ReactiveStringRedisTemplate.class);
            ReactiveValueOperations<String, String> ops = Mockito.mock(ReactiveValueOperations.class);

            when(mockTemplate.opsForValue()).thenReturn(ops);

            when(ops.increment(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                long val = redisStore.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
                return Mono.just(val);
            });

            when(mockTemplate.expire(anyString(), any(Duration.class))).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                Duration dur = invocation.getArgument(1);
                redisTtl.put(key, dur.toSeconds());
                return Mono.just(true);
            });

            return mockTemplate;
        }
    }

    @BeforeEach
    void setUp() {
        redisStore.clear();
        redisTtl.clear();
    }

    @Test
    @Order(1)
    @DisplayName("RateLimit E2E: 5 requests allowed -> 6th triggers HTTP 429 with JSON body & Retry-After")
    void rateLimit_burstOf6_triggers429On6th() {
        long initialEvents = securityEventRepository.count();

        // Requests 1 through 5 must be allowed (HTTP 200)
        for (int i = 1; i <= 5; i++) {
            webTestClient.get().uri("/api/v1/system/health")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals("X-RateLimit-Limit", "5")
                    .expectHeader().valueEquals("X-RateLimit-Remaining", String.valueOf(5 - i))
                    .expectHeader().exists("X-RateLimit-Reset")
                    .expectHeader().exists("Retry-After");
        }

        // Verify Redis state after 5 requests
        assertThat(redisStore).isNotEmpty();
        String activeKey = redisStore.keySet().iterator().next();
        assertThat(activeKey).startsWith("rl:ip:");
        assertThat(redisStore.get(activeKey).get()).isEqualTo(5L);
        assertThat(redisTtl.get(activeKey)).isEqualTo(60L);

        // Request 6 must be rejected with HTTP 429 TOO_MANY_REQUESTS
        webTestClient.get().uri("/api/v1/system/health")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectHeader().valueEquals("X-RateLimit-Limit", "5")
                .expectHeader().valueEquals("X-RateLimit-Remaining", "0")
                .expectHeader().exists("Retry-After")
                .expectBody()
                .jsonPath("$.error").isEqualTo("Rate limit exceeded")
                .jsonPath("$.retryAfterSeconds").isNumber();

        // Verify Redis counter reached 6
        assertThat(redisStore.get(activeKey).get()).isEqualTo(6L);

        // Verify Security Event was recorded in PostgreSQL / H2 database
        long postEvents = securityEventRepository.count();
        assertThat(postEvents).isGreaterThan(initialEvents);

        boolean rateLimitEventFound = securityEventRepository.findAll().stream()
                .anyMatch(e -> e.getEventType() == EventType.RATE_LIMIT_EXCEEDED
                        && e.getSeverity() == SeverityLevel.MEDIUM
                        && e.getActionTaken().equals("HTTP_429_TOO_MANY_REQUESTS"));
        assertThat(rateLimitEventFound).isTrue();

        // Verify Analytics overview reflects the rate limit hit (queried by unthrottled admin identity)
        webTestClient.get().uri("/api/v1/analytics/overview")
                .header("X-User-Name", "admin_monitor")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.rateLimitHits").isNumber();
    }

    @Test
    @Order(2)
    @DisplayName("RateLimit E2E: Identity Isolation — Exceeding limit for User A does NOT block User B")
    void rateLimit_identityIsolation_userBNotBlocked() {
        // User A exhausts rate limit (5 requests)
        for (int i = 1; i <= 5; i++) {
            webTestClient.get().uri("/api/v1/system/health")
                    .header("X-User-Name", "alice")
                    .exchange()
                    .expectStatus().isOk();
        }

        // User A 6th request is blocked (HTTP 429)
        webTestClient.get().uri("/api/v1/system/health")
                .header("X-User-Name", "alice")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // User B (independent identity) sends Request 1 -> must be allowed (HTTP 200)
        webTestClient.get().uri("/api/v1/system/health")
                .header("X-User-Name", "bob")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-RateLimit-Remaining", "4");

        // Verify separate Redis keys were created for alice and bob
        boolean aliceKeyExists = redisStore.keySet().stream().anyMatch(k -> k.startsWith("rl:user:alice:"));
        boolean bobKeyExists = redisStore.keySet().stream().anyMatch(k -> k.startsWith("rl:user:bob:"));
        assertThat(aliceKeyExists).isTrue();
        assertThat(bobKeyExists).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("RateLimit E2E: Window Reset — Requests allowed again after window expiry")
    void rateLimit_windowReset_allowedAgain() {
        // Exhaust limit for identity
        for (int i = 1; i <= 6; i++) {
            webTestClient.get().uri("/api/v1/system/health")
                    .header("X-Client-Id", "sg_live_prodclient")
                    .exchange();
        }

        // Currently blocked (429)
        webTestClient.get().uri("/api/v1/system/health")
                .header("X-Client-Id", "sg_live_prodclient")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Simulate sliding window expiration by clearing the window counter
        redisStore.clear();

        // Next request in new window must be allowed (200 OK)
        webTestClient.get().uri("/api/v1/system/health")
                .header("X-Client-Id", "sg_live_prodclient")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-RateLimit-Remaining", "4");
    }
}
