package com.sentinelgate.service;

import com.sentinelgate.dto.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService(redisTemplate);
    }

    @Test
    @DisplayName("Should allow request when count is within configured threshold")
    void checkRateLimit_Allowed() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(5L));

        Mono<RateLimitResult> resultMono = rateLimitingService.checkRateLimit("ip:192.168.1.1:/api/v1/orders", 100, 60);

        StepVerifier.create(resultMono)
                .expectNextMatches(result -> result.getAllowed() && result.getCurrentCount() == 5L && result.getLimit() == 100L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should reject request when count exceeds configured threshold")
    void checkRateLimit_Exceeded() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(101L));

        Mono<RateLimitResult> resultMono = rateLimitingService.checkRateLimit("ip:192.168.1.1:/api/v1/orders", 100, 60);

        StepVerifier.create(resultMono)
                .expectNextMatches(result -> !result.getAllowed() && result.getCurrentCount() == 101L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should set key TTL expiration on first request in window")
    void checkRateLimit_SetsExpirationOnFirstRequest() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        Mono<RateLimitResult> resultMono = rateLimitingService.checkRateLimit("ip:192.168.1.1:/api/v1/orders", 100, 60);

        StepVerifier.create(resultMono)
                .expectNextMatches(result -> result.getAllowed() && result.getCurrentCount() == 1L)
                .verifyComplete();

        verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(60)));
    }
}
