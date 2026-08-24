package com.sentinelgate.dto;

import com.sentinelgate.domain.enums.ServiceStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackendServiceDto {
    private Long id;
    private String name;
    private String baseUrl;
    private String healthEndpoint;
    private ServiceStatus status;
    private String description;
    private Instant createdAt;
}
