package com.sentinelgate.domain;

import com.sentinelgate.domain.enums.ServiceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "backend_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackendService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "base_url", nullable = false, length = 255)
    private String baseUrl;

    @Builder.Default
    @Column(name = "health_endpoint", length = 100)
    private String healthEndpoint = "/actuator/health";

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20)
    private ServiceStatus status = ServiceStatus.HEALTHY;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
