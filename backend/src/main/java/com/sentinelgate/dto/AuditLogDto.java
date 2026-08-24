package com.sentinelgate.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDto {
    private Long id;
    private String actorUsername;
    private String action;
    private String resource;
    private String details;
    private String sourceIp;
    private Instant timestamp;
}
