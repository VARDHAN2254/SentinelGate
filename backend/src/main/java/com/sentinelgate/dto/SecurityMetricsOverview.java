package com.sentinelgate.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityMetricsOverview {
    private Long totalSecurityEvents;
    private Long blockedRequests;
    private Long authFailures;
    private Long bruteForceEvents;
    private Long suspiciousIps;
    private Long rateLimitHits;
    private Long recentEventsLastHour;
    private Double requestsPerSecond;
    private Long activeRoutes;
    private Long activeApiKeys;
}
