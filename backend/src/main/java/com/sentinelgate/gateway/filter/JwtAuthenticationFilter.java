package com.sentinelgate.gateway.filter;

import com.sentinelgate.security.JwtTokenProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway GlobalFilter that enriches proxied requests with user context headers.
 * Downstream services receive X-User-Name and X-User-Role without needing to
 * validate the JWT themselves.
 *
 * <p>Authorization enforcement (hasAuthority rules) is handled by
 * {@link com.sentinelgate.security.JwtSecurityContextFilter}, which runs as a
 * WebFilter before Spring Security evaluates access rules.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return chain.filter(exchange);
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        String role = jwtTokenProvider.getRoleFromToken(token);

        ServerHttpRequest enrichedRequest = exchange.getRequest().mutate()
                .header("X-User-Name", username)
                .header("X-User-Role", role != null ? role : "VIEWER")
                .build();

        return chain.filter(exchange.mutate().request(enrichedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
