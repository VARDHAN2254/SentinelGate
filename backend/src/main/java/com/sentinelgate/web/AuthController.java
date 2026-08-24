package com.sentinelgate.web;

import com.sentinelgate.dto.LoginRequest;
import com.sentinelgate.dto.RegisterRequest;
import com.sentinelgate.security.JwtTokenProvider;
import com.sentinelgate.service.AuthService;
import com.sentinelgate.service.SecurityDetectionEngineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityDetectionEngineService securityEngine;

    public AuthController(AuthService authService,
                          JwtTokenProvider jwtTokenProvider,
                          SecurityDetectionEngineService securityEngine) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.securityEngine = securityEngine;
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<Object>> register(@RequestBody RegisterRequest request) {
        return Mono.fromCallable(() -> authService.register(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(user -> ResponseEntity.status(HttpStatus.CREATED).<Object>body(user))
                .onErrorResume(IllegalArgumentException.class, ex -> {
                    String msg = ex.getMessage();
                    // Uniqueness violations are 409 Conflict, not 400 Bad Request
                    if (msg != null && (msg.contains("already taken") || msg.contains("already registered"))) {
                        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                                .<Object>body(Map.of("error", msg)));
                    }
                    return Mono.just(ResponseEntity.badRequest().<Object>body(Map.of("error", msg)));
                });
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<Object>> login(@RequestBody LoginRequest request, ServerHttpRequest httpRequest) {
        InetSocketAddress remoteAddress = httpRequest.getRemoteAddress();
        String clientIp = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "UNKNOWN";

        return Mono.fromCallable(() -> authService.login(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(jwt -> ResponseEntity.ok().<Object>body(jwt))
                .onErrorResume(IllegalArgumentException.class, ex ->
                        securityEngine.evaluateAuthFailure(clientIp, request.getUsername(), "/api/v1/auth/login")
                                .then(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .<Object>body(Map.of("error", "Invalid username or password")))))
                .onErrorResume(IllegalStateException.class, ex ->
                        Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .<Object>body(Map.of("error", ex.getMessage()))));
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<Object>> getCurrentUser(
            @RequestHeader(name = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .<Object>body(Map.of("error", "Missing or invalid Authorization header")));
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .<Object>body(Map.of("error", "Invalid or expired JWT token")));
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        return Mono.fromCallable(() -> authService.getUserProfile(username))
                .subscribeOn(Schedulers.boundedElastic())
                .map(profile -> ResponseEntity.ok().<Object>body(profile))
                .onErrorResume(IllegalArgumentException.class, ex ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .<Object>body(Map.of("error", ex.getMessage()))));
    }
}
