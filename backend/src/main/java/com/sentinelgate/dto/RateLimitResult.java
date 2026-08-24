package com.sentinelgate.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitResult {
    private Boolean allowed;
    private Long currentCount;
    private Long limit;
    private Long resetSeconds;
}
