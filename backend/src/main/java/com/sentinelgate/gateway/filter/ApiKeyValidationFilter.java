package com.sentinelgate.gateway.filter;

import com.sentinelgate.domain.ApiKey;
import com.sentinelgate.domain.enums.ApiKeyStatus;
import com.sentinelgate.repository.ApiKeyRepository;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Validates X-API-KEY header for machine-to-machine clients across all incoming requests.
 * Expected format: {@code sg_live_<8-hex-prefix>_<24-hex-secret>}
 *
 * <p>All database operations run on the bounded-elastic scheduler to
 * avoid blocking Netty's I/O event loop.
 */
@Component
@Order(-90)
public class ApiKeyValidationFilter implements WebFilter, Ordered {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyValidationFilter(ApiKeyRepository apiKeyRepository, PasswordEncoder passwordEncoder) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String apiKeyHeader = exchange.getRequest().getHeaders().getFirst("X-API-KEY");

        if (apiKeyHeader == null || !apiKeyHeader.startsWith("sg_live_")) {
            return chain.filter(exchange);
        }

        // Minimum viable length: "sg_live_" (8) + prefix (8) + "_" + secret (24) = 41
        if (apiKeyHeader.length() < 41) {
            return rejectRequest(exchange, HttpStatus.UNAUTHORIZED, "Malformed API key format");
        }

        // The prefix is the first 16 characters: sg_live_ + 8 hex chars
        String keyPrefix = apiKeyHeader.substring(0, 16);

        return Mono.fromCallable(() -> apiKeyRepository.findByKeyPrefix(keyPrefix))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optionalKey -> {
                    if (optionalKey.isEmpty()) {
                        return rejectRequest(exchange, HttpStatus.UNAUTHORIZED, "Invalid API key");
                    }

                    ApiKey apiKey = optionalKey.get();

                    if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
                        return rejectRequest(exchange, HttpStatus.FORBIDDEN,
                                "API key is " + apiKey.getStatus().name().toLowerCase());
                    }

                    if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(Instant.now())) {
                        return rejectRequest(exchange, HttpStatus.FORBIDDEN, "API key has expired");
                    }

                    if (!passwordEncoder.matches(apiKeyHeader, apiKey.getKeyHash())) {
                        return rejectRequest(exchange, HttpStatus.UNAUTHORIZED, "Invalid API key");
                    }

                    // Update last-used timestamp off the event loop
                    Mono<Void> updateLastUsed = Mono.fromRunnable(() -> {
                        apiKey.setLastUsedAt(Instant.now());
                        apiKeyRepository.save(apiKey);
                    }).subscribeOn(Schedulers.boundedElastic()).then();

                    ServerHttpRequest enrichedRequest = exchange.getRequest().mutate()
                            .header("X-Client-Id", apiKey.getKeyPrefix())
                            .header("X-User-Role", "MACHINE_CLIENT")
                            .build();

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            apiKey.getKeyPrefix(),
                            null,
                            List.of(new SimpleGrantedAuthority("MACHINE_CLIENT"))
                    );

                    return chain.filter(exchange.mutate().request(enrichedRequest).build())
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                            .then(updateLastUsed);
                });
    }

    private Mono<Void> rejectRequest(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
