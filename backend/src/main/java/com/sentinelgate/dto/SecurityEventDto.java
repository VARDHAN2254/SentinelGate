package com.sentinelgate.dto;

import com.sentinelgate.domain.enums.EventType;
import com.sentinelgate.domain.enums.SeverityLevel;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEventDto {
    private Long id;
    private String eventUuid;
    private EventType eventType;
    private SeverityLevel severity;
    private String sourceIp;
    private String clientIdentity;
    private String endpoint;
    private String httpMethod;
    private String description;
    private String ruleTriggered;
    private String actionTaken;
    private Instant timestamp;
}
