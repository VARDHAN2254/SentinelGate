package com.sentinelgate.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter totalRequestsCounter(MeterRegistry registry) {
        return Counter.builder("sentinelgate.requests.total")
                .description("Total HTTP API requests processed by SentinelGate API Gateway")
                .register(registry);
    }

    @Bean
    public Counter rateLimitViolationsCounter(MeterRegistry registry) {
        return Counter.builder("sentinelgate.ratelimit.violations.total")
                .description("Total rate limit throttling events enforced by Redis rate limiter")
                .register(registry);
    }

    @Bean
    public Counter securityThreatsCounter(MeterRegistry registry) {
        return Counter.builder("sentinelgate.security.threats.total")
                .description("Total security threat events detected by SentinelGate security engine")
                .register(registry);
    }
}
