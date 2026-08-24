package com.sentinelgate.gateway.filter;

import com.sentinelgate.domain.enums.EventType;
import com.sentinelgate.domain.enums.SeverityLevel;
import com.sentinelgate.service.RateLimitingService;
import com.sentinelgate.service.SecurityDetectionEngineService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Enforces per-subject rate limits using Redis sliding windows across all incoming requests.
 *
 * <p>Subject resolution priority:
 * <ol>
 *   <li>API key clients (X-Client-Id header) — default 1000 req/min</li>
 *   <li>Authenticated users (X-User-Name header) — default 300 req/min</li>
 *   <li>Anonymous IP — default 100 req/min</li>
 * </ol>
 *
 * <p>Auth endpoints (/api/v1/auth/login, /register) get a separate,
 * configurable limit (default 20 req/min per IP) to slow credential-stuffing attacks.
 */
@Component
@Order(-80)
public class RateLimiterFilter implements WebFilter, Ordered {

    @Value("${sentinelgate.ratelimit.auth-limit:20}")
    private int authEndpointLimit;

    @Value("${sentinelgate.ratelimit.ip-limit:100}")
    private int anonymousIpLimit;

    @Value("${sentinelgate.ratelimit.user-limit:300}")
    private int authenticatedUserLimit;

    @Value("${sentinelgate.ratelimit.api-key-limit:1000}")
    private int apiKeyLimit;

    private final RateLimitingService rateLimitingService;
    private final SecurityDetectionEngineService securityEngine;
    private final MeterRegistry meterRegistry;

    public RateLimiterFilter(RateLimitingService rateLimitingService,
                             SecurityDetectionEngineService securityEngine,
                             MeterRegistry meterRegistry) {
        this.rateLimitingService = rateLimitingService;
        this.securityEngine = securityEngine;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Actuator endpoints are internal infrastructure — exclude from rate limiting
        if (path.startsWith("/actuator/")) {
            return chain.filter(exchange);
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        String clientIp = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";

        // Auth endpoints get a dedicated lower per-IP limit to deter credential stuffing.
        if (path.startsWith("/api/v1/auth/")) {
            String authKey = "auth-ip:" + clientIp;
            return rateLimitingService.checkRateLimit(authKey, authEndpointLimit, 60)
                    .flatMap(result -> {
                        addRateLimitHeaders(exchange, result.getLimit(), result.getCurrentCount(), result.getResetSeconds());
                        if (!result.getAllowed()) {
                            return rejectWithTooManyRequests(exchange, clientIp, null, path,
                                    exchange.getRequest().getMethod().name(),
                                    result.getCurrentCount(), result.getLimit(), result.getResetSeconds());
                        }
                        return chain.filter(exchange);
                    });
        }

        // Determine the rate-limit subject for non-auth traffic.
        // Key is scoped to the subject identity only — not per-path — so burst
        // protection applies across all endpoints the same client touches.
        String apiKeyPrefix = exchange.getRequest().getHeaders().getFirst("X-Client-Id");
        String username = exchange.getRequest().getHeaders().getFirst("X-User-Name");

        String rateLimitSubject;
        int limit;

        if (apiKeyPrefix != null) {
            rateLimitSubject = "apikey:" + apiKeyPrefix;
            limit = apiKeyLimit;
        } else if (username != null) {
            rateLimitSubject = "user:" + username;
            limit = authenticatedUserLimit;
        } else {
            rateLimitSubject = "ip:" + clientIp;
            limit = anonymousIpLimit;
        }

        return rateLimitingService.checkRateLimit(rateLimitSubject, limit, 60)
                .flatMap(result -> {
                    addRateLimitHeaders(exchange, result.getLimit(), result.getCurrentCount(), result.getResetSeconds());

                    if (!result.getAllowed()) {
                        String subjectType = rateLimitSubject.split(":")[0];
                        Counter.builder("sentinelgate.ratelimit.violations.total")
                                .tag("subject", subjectType)
                                .register(meterRegistry)
                                .increment();

                        return rejectWithTooManyRequests(exchange, clientIp,
                                username != null ? username : apiKeyPrefix,
                                path, exchange.getRequest().getMethod().name(),
                                result.getCurrentCount(), result.getLimit(), result.getResetSeconds());
                    }

                    return chain.filter(exchange);
                });
    }

    private void addRateLimitHeaders(ServerWebExchange exchange, long limit, long current, long resetSeconds) {
        long remaining = Math.max(0, limit - current);
        exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(limit));
        exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(remaining));
        exchange.getResponse().getHeaders().set("X-RateLimit-Reset", String.valueOf(resetSeconds));
        exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(resetSeconds));
    }

    private Mono<Void> rejectWithTooManyRequests(ServerWebExchange exchange, String clientIp,
                                                  String clientIdentity, String path, String method,
                                                  long currentCount, long limit, long resetSeconds) {
        securityEngine.recordSecurityEvent(
                EventType.RATE_LIMIT_EXCEEDED,
                SeverityLevel.MEDIUM,
                clientIp,
                clientIdentity,
                path,
                method,
                "Rate limit breached: " + currentCount + "/" + limit + " req/min",
                "RATE_LIMIT_RULE",
                "HTTP_429_TOO_MANY_REQUESTS"
        );

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("Retry-After", String.valueOf(resetSeconds));

        String body = String.format(
                "{\"error\":\"Rate limit exceeded\",\"retryAfterSeconds\":%d}", resetSeconds);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -80;
    }
}
