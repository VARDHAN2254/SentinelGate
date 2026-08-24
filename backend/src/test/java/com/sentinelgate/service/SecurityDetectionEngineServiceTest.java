package com.sentinelgate.service;

import com.sentinelgate.domain.SecurityEvent;
import com.sentinelgate.domain.enums.EventType;
import com.sentinelgate.domain.enums.SeverityLevel;
import com.sentinelgate.repository.SecurityEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityDetectionEngineServiceTest {

    @Mock
    private SecurityEventRepository eventRepository;

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    private MeterRegistry meterRegistry;
    private SecurityDetectionEngineService securityEngine;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        securityEngine = new SecurityDetectionEngineService(eventRepository, redisTemplate, meterRegistry);
        lenient().when(eventRepository.save(any(SecurityEvent.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Should successfully record security threat event in datastore")
    void recordSecurityEvent_Success() {
        SecurityEvent event = securityEngine.recordSecurityEvent(
                EventType.UNAUTHORIZED_ACCESS,
                SeverityLevel.HIGH,
                "192.168.1.50",
                "attacker",
                "/api/v1/admin/services",
                "POST",
                "Unauthorized admin operation attempt",
                "RBAC_ENFORCER",
                "HTTP_403_FORBIDDEN"
        );

        assertNotNull(event);
        assertNotNull(event.getEventUuid());
        assertEquals(EventType.UNAUTHORIZED_ACCESS, event.getEventType());
        assertEquals(SeverityLevel.HIGH, event.getSeverity());
        assertEquals("192.168.1.50", event.getSourceIp());
        verify(eventRepository).save(any(SecurityEvent.class));
    }

    @Test
    @DisplayName("Should trigger BRUTE_FORCE alert when failed login count reaches threshold 5")
    void evaluateAuthFailure_TriggersBruteForce() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(5L));

        Mono<Void> mono = securityEngine.evaluateAuthFailure("192.168.1.100", "victim_user", "/api/v1/auth/login");

        StepVerifier.create(mono).verifyComplete();

        // 1 AUTH_FAILURE event + 1 BRUTE_FORCE event recorded
        verify(eventRepository, times(2)).save(any(SecurityEvent.class));
    }
}
