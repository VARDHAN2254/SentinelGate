package com.sentinelgate.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityRuleDto {
    private Long id;
    private String ruleName;
    private String ruleType;
    private Integer thresholdCount;
    private Integer windowSeconds;
    private String actionTaken;
    private Boolean enabled;
    private Instant createdAt;
}
