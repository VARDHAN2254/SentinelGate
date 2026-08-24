package com.sentinelgate.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * A Spring WebFlux {@link WebFilter} that runs before Spring Security's own
 * evaluation chain. It extracts the JWT from the Authorization header and
 * populates the Spring Security reactive context with the authenticated principal.
 *
 * <p>This is distinct from the {@code JwtAuthenticationFilter} GlobalFilter:
 * <ul>
 *   <li>This WebFilter sets the security context so Spring Security's
 *       {@code .hasAuthority()} rules in {@code SecurityConfig} are satisfied.</li>
 *   <li>The GlobalFilter enriches downstream request headers (X-User-Name, X-User-Role)
 *       for proxied backend services.</li>
 * </ul>
 */
@Component
public class JwtSecurityContextFilter implements WebFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtSecurityContextFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
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
        String effectiveRole = role != null ? role : "VIEWER";

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(effectiveRole))
        );

        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }
}
