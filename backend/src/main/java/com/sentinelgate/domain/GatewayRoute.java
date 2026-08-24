package com.sentinelgate.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "gateway_routes", indexes = {
        @Index(name = "idx_routes_pattern", columnList = "path_pattern")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false, unique = true, length = 100)
    private String routeId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private BackendService service;

    @Column(name = "path_pattern", nullable = false, length = 255)
    private String pathPattern;

    @Column(name = "target_path_prefix", length = 255)
    private String targetPathPrefix;

    @Builder.Default
    @Column(name = "requires_auth")
    private Boolean requiresAuth = true;

    @Column(name = "allowed_roles", length = 255)
    private String allowedRoles; // Comma separated e.g. "ADMIN,DEVELOPER"

    @Builder.Default
    @Column(name = "rate_limit_per_min")
    private Integer rateLimitPerMin = 100;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
