package com.sentinelgate.dto;

import com.sentinelgate.domain.enums.ApiKeyStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApiKeyResponse {
    private Long id;
    private String name;
    private String keyPrefix;
    private String rawKey; // RETURNED ONCE UPON CREATION
    private ApiKeyStatus status;
    private Integer rateLimitPerMin;
    private Instant expiresAt;
    private Instant createdAt;
}
