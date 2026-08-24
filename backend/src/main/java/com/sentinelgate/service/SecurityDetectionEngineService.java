package com.sentinelgate.service;

import com.sentinelgate.domain.SecurityEvent;
import com.sentinelgate.domain.enums.EventType;
import com.sentinelgate.domain.enums.SeverityLevel;
import com.sentinelgate.dto.SecurityEventDto;
import com.sentinelgate.repository.SecurityEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class SecurityDetectionEngineService {

    private static final Logger log = LoggerFactory.getLogger(SecurityDetectionEngineService.class);

    private final SecurityEventRepository eventRepository;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    public SecurityDetectionEngineService(SecurityEventRepository eventRepository,
                                         ReactiveStringRedisTemplate redisTemplate,
                                         MeterRegistry meterRegistry) {
        this.eventRepository = eventRepository;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public SecurityEvent recordSecurityEvent(EventType eventType,
                                             SeverityLevel severity,
                                             String sourceIp,
                                             String clientIdentity,
                                             String endpoint,
                                             String httpMethod,
                                             String description,
                                             String ruleTriggered,
                                             String actionTaken) {

        SecurityEvent event = SecurityEvent.builder()
                .eventUuid(UUID.randomUUID().toString())
                .eventType(eventType)
                .severity(severity)
                .sourceIp(sourceIp != null ? sourceIp : "UNKNOWN")
                .clientIdentity(clientIdentity)
                .endpoint(endpoint != null ? endpoint : "/api")
                .httpMethod(httpMethod != null ? httpMethod : "GET")
                .description(description)
                .ruleTriggered(ruleTriggered)
                .actionTaken(actionTaken)
                .timestamp(Instant.now())
                .build();

        SecurityEvent saved = eventRepository.save(event);

        Counter.builder("sentinelgate.security.threats.total")
                .tag("event_type", eventType.name())
                .tag("severity", severity.name())
                .register(meterRegistry)
                .increment();

        log.warn("[SECURITY THREAT DETECTED] type={} severity={} ip={} endpoint={} rule={}",
                eventType, severity, sourceIp, endpoint, ruleTriggered);

        return saved;
    }

    public Mono<Void> evaluateAuthFailure(String sourceIp, String username, String endpoint) {
        String redisKey = "bf:" + sourceIp + ":" + (username != null ? username : "unknown");

        return redisTemplate.opsForValue().increment(redisKey)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(redisKey, Duration.ofSeconds(300)).thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .flatMap(count -> {
                    recordSecurityEvent(
                            EventType.AUTH_FAILURE, SeverityLevel.MEDIUM, sourceIp, username,
                            endpoint, "POST", "Authentication failed for user: " + username,
                            "AUTH_FAILURE_RULE", "LOG_AND_COUNT");

                    if (count >= 5) {
                        recordSecurityEvent(
                                EventType.BRUTE_FORCE, SeverityLevel.HIGH, sourceIp, username,
                                endpoint, "POST",
                                "Brute-force pattern from IP: " + sourceIp + " (" + count + " failures in 300s)",
                                "BRUTE_FORCE_RULE", "BLOCK_TEMPORARY");
                    }
                    return Mono.<Void>empty();
                })
                // Graceful degradation: if Redis is unavailable, still record the auth failure to DB
                .onErrorResume(ex -> {
                    log.warn("[DETECTION] Redis unavailable for brute-force tracking, recording AUTH_FAILURE directly: {}", ex.getMessage());
                    recordSecurityEvent(
                            EventType.AUTH_FAILURE, SeverityLevel.MEDIUM, sourceIp, username,
                            endpoint, "POST", "Authentication failed for user: " + username,
                            "AUTH_FAILURE_RULE", "LOG_AND_COUNT");
                    return Mono.empty();
                })
                .then();
    }

    @Transactional(readOnly = true)
    public Page<SecurityEventDto> getRecentEvents(int page, int size,
                                                   SeverityLevel severityFilter,
                                                   EventType typeFilter) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<SecurityEvent> events;

        if (severityFilter != null) {
            events = eventRepository.findBySeverityOrderByTimestampDesc(severityFilter, pageRequest);
        } else if (typeFilter != null) {
            events = eventRepository.findByEventTypeOrderByTimestampDesc(typeFilter, pageRequest);
        } else {
            events = eventRepository.findAllByOrderByTimestampDesc(pageRequest);
        }

        return events.map(this::mapToDto);
    }

    private SecurityEventDto mapToDto(SecurityEvent event) {
        return SecurityEventDto.builder()
                .id(event.getId())
                .eventUuid(event.getEventUuid())
                .eventType(event.getEventType())
                .severity(event.getSeverity())
                .sourceIp(event.getSourceIp())
                .clientIdentity(event.getClientIdentity())
                .endpoint(event.getEndpoint())
                .httpMethod(event.getHttpMethod())
                .description(event.getDescription())
                .ruleTriggered(event.getRuleTriggered())
                .actionTaken(event.getActionTaken())
                .timestamp(event.getTimestamp())
                .build();
    }
}
