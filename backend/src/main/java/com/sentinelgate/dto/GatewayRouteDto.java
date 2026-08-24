package com.sentinelgate.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayRouteDto {
    private Long id;
    private String routeId;
    private Long serviceId;
    private String serviceName;
    private String serviceBaseUrl;
    private String pathPattern;
    private String targetPathPrefix;
    private Boolean requiresAuth;
    private String allowedRoles;
    private Integer rateLimitPerMin;
    private Boolean isActive;
    private Instant createdAt;
}
