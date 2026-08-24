package com.sentinelgate.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApiKeyRequest {
    private String name;
    private Integer rateLimitPerMin;
    private Instant expiresAt;
}
