package com.sentinelgate.dto;

import com.sentinelgate.domain.enums.ApiKeyStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyDto {
    private Long id;
    private String name;
    private String keyPrefix;
    private String ownerUsername;
    private ApiKeyStatus status;
    private Integer rateLimitPerMin;
    private Instant expiresAt;
    private Instant lastUsedAt;
    private Instant createdAt;
}
