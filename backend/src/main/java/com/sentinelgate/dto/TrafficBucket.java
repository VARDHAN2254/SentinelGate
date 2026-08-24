package com.sentinelgate.dto;

import lombok.*;

/**
 * Represents one 5-minute window in the gateway traffic timeline.
 * Used by GET /api/v1/analytics/traffic-timeline to power the dashboard chart.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficBucket {
    /** ISO-8601 window start time, e.g. "2024-08-24T18:00:00Z" */
    private String windowStart;
    /** Total security events recorded in this window */
    private long totalEvents;
    /** Events that resulted in a block (rate limit or brute force) */
    private long blockedEvents;
    /** Authentication failures recorded in this window */
    private long authFailures;
}
