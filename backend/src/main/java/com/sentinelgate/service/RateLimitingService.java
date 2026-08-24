package com.sentinelgate.service;

import com.sentinelgate.dto.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RateLimitingService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);
    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitingService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<RateLimitResult> checkRateLimit(String rateLimitKey, int limit, int windowSeconds) {
        long currentWindow = System.currentTimeMillis() / (windowSeconds * 1000L);
        String redisKey = "rl:" + rateLimitKey + ":" + currentWindow;

        return redisTemplate.opsForValue().increment(redisKey)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds))
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .map(count -> {
                    boolean allowed = count <= limit;
                    long remaining = Math.max(0, limit - count);
                    long resetSeconds = windowSeconds - ((System.currentTimeMillis() / 1000L) % windowSeconds);

                    return RateLimitResult.builder()
                            .allowed(allowed)
                            .currentCount(count)
                            .limit((long) limit)
                            .resetSeconds(resetSeconds)
                            .build();
                })
                .onErrorResume(ex -> {
                    log.warn("Redis rate limiter fallback due to exception: {}", ex.getMessage());
                    // Fallback to allow traffic if Redis is unreachable
                    return Mono.just(RateLimitResult.builder()
                            .allowed(true)
                            .currentCount(1L)
                            .limit((long) limit)
                            .resetSeconds((long) windowSeconds)
                            .build());
                });
    }
}
