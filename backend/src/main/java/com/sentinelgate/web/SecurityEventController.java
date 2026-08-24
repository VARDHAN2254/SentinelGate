package com.sentinelgate.web;

import com.sentinelgate.domain.enums.EventType;
import com.sentinelgate.domain.enums.SeverityLevel;
import com.sentinelgate.dto.SecurityEventDto;
import com.sentinelgate.service.SecurityDetectionEngineService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/analytics/events")
public class SecurityEventController {

    private final SecurityDetectionEngineService securityEngine;

    public SecurityEventController(SecurityDetectionEngineService securityEngine) {
        this.securityEngine = securityEngine;
    }

    @GetMapping
    public Mono<ResponseEntity<Page<SecurityEventDto>>> getSecurityEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String eventType) {

        SeverityLevel severityFilter = severity != null ? SeverityLevel.valueOf(severity.toUpperCase()) : null;
        EventType typeFilter = eventType != null ? EventType.valueOf(eventType.toUpperCase()) : null;

        return Mono.fromCallable(() -> securityEngine.getRecentEvents(page, size, severityFilter, typeFilter))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }
}
