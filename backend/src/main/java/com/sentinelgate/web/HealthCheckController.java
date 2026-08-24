package com.sentinelgate.web;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class HealthCheckController {

    private final DataSource dataSource;
    private final ReactiveStringRedisTemplate redisTemplate;

    public HealthCheckController(DataSource dataSource, ReactiveStringRedisTemplate redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> getSystemHealth() {
        return Mono.defer(() -> redisTemplate.getConnectionFactory().getReactiveConnection().ping())
                .timeout(Duration.ofSeconds(2))
                .map(pong -> "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN")
                .onErrorReturn("DOWN")
                .defaultIfEmpty("DOWN")
                .map(redisStatus -> {
                    Map<String, Object> health = new HashMap<>();
                    health.put("status", "UP");
                    health.put("timestamp", Instant.now().toString());
                    health.put("service", "SentinelGate API Gateway");
                    
                    Map<String, String> components = new HashMap<>();
                    components.put("redis", redisStatus);
                    components.put("database", checkDatabaseConnection());
                    components.put("gateway", "UP");
                    
                    health.put("components", components);
                    return ResponseEntity.ok(health);
                });
    }

    private String checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
