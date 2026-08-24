package com.sentinelgate.web;

import com.sentinelgate.domain.enums.ApiKeyStatus;
import com.sentinelgate.domain.enums.EventType;
import com.sentinelgate.dto.SecurityMetricsOverview;
import com.sentinelgate.dto.TrafficBucket;
import com.sentinelgate.repository.ApiKeyRepository;
import com.sentinelgate.repository.GatewayRouteRepository;
import com.sentinelgate.repository.SecurityEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);

    private final SecurityEventRepository eventRepository;
    private final GatewayRouteRepository routeRepository;
    private final ApiKeyRepository apiKeyRepository;

    public AnalyticsController(SecurityEventRepository eventRepository,
                                GatewayRouteRepository routeRepository,
                                ApiKeyRepository apiKeyRepository) {
        this.eventRepository = eventRepository;
        this.routeRepository = routeRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * Returns real security metrics derived entirely from the database.
     * No values are hardcoded or inflated.
     */
    @GetMapping("/overview")
    public Mono<ResponseEntity<SecurityMetricsOverview>> getMetricsOverview() {
        return Mono.fromCallable(() -> {
            long authFailures = eventRepository.countByEventType(EventType.AUTH_FAILURE);
            long rateLimitHits = eventRepository.countByEventType(EventType.RATE_LIMIT_EXCEEDED);
            long bruteForceEvents = eventRepository.countByEventType(EventType.BRUTE_FORCE);
            long suspiciousIps = eventRepository.countByEventType(EventType.SUSPICIOUS_IP);
            long blockedRequests = rateLimitHits + bruteForceEvents;

            Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
            long recentEvents = eventRepository.countByTimestampAfter(oneHourAgo);
            // RPS is events observed in the last hour divided by 3600 seconds
            double rps = Math.round((recentEvents / 3600.0) * 10.0) / 10.0;

            long activeApiKeys = apiKeyRepository.countByStatus(ApiKeyStatus.ACTIVE);

            SecurityMetricsOverview overview = SecurityMetricsOverview.builder()
                    .totalSecurityEvents(eventRepository.count())
                    .blockedRequests(blockedRequests)
                    .authFailures(authFailures)
                    .bruteForceEvents(bruteForceEvents)
                    .suspiciousIps(suspiciousIps)
                    .rateLimitHits(rateLimitHits)
                    .recentEventsLastHour(recentEvents)
                    .requestsPerSecond(rps)
                    .activeRoutes((long) routeRepository.findByIsActiveTrue().size())
                    .activeApiKeys(activeApiKeys)
                    .build();

            return ResponseEntity.ok(overview);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Returns gateway traffic bucketed into 5-minute windows over the last hour.
     * Each bucket is populated from the security_events table — not simulated.
     *
     * <p>Note: security_events only records events that triggered a rule (auth failures,
     * rate limit violations, brute-force detections). It is not a full request log.
     * A full request log would require a dedicated access-log table.
     */
    @GetMapping("/traffic-timeline")
    public Mono<ResponseEntity<List<TrafficBucket>>> getTrafficTimeline() {
        return Mono.fromCallable(() -> {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
            List<TrafficBucket> buckets = new ArrayList<>();

            // Build 12 five-minute buckets covering the last hour
            for (int i = 11; i >= 0; i--) {
                Instant windowStart = now.minus(i * 5L + 5, ChronoUnit.MINUTES);
                Instant windowEnd = now.minus(i * 5L, ChronoUnit.MINUTES);

                long total = eventRepository.countByTimestampBetween(windowStart, windowEnd);
                long blocked = eventRepository.countByEventTypeAndTimestampBetween(
                        EventType.RATE_LIMIT_EXCEEDED, windowStart, windowEnd)
                        + eventRepository.countByEventTypeAndTimestampBetween(
                        EventType.BRUTE_FORCE, windowStart, windowEnd);
                long authFails = eventRepository.countByEventTypeAndTimestampBetween(
                        EventType.AUTH_FAILURE, windowStart, windowEnd);

                buckets.add(TrafficBucket.builder()
                        .windowStart(ISO_FORMATTER.format(windowStart))
                        .totalEvents(total)
                        .blockedEvents(blocked)
                        .authFailures(authFails)
                        .build());
            }

            return ResponseEntity.ok(buckets);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
