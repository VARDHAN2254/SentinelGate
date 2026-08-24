package com.sentinelgate.service;

import com.sentinelgate.domain.BackendService;
import com.sentinelgate.domain.GatewayRoute;
import com.sentinelgate.domain.enums.ServiceStatus;
import com.sentinelgate.dto.BackendServiceDto;
import com.sentinelgate.dto.GatewayRouteDto;
import com.sentinelgate.repository.BackendServiceRepository;
import com.sentinelgate.repository.GatewayRouteRepository;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GatewayRouteService {

    private final BackendServiceRepository serviceRepository;
    private final GatewayRouteRepository routeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public GatewayRouteService(BackendServiceRepository serviceRepository,
                               GatewayRouteRepository routeRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.serviceRepository = serviceRepository;
        this.routeRepository = routeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<BackendServiceDto> getAllServices() {
        return serviceRepository.findAll().stream()
                .map(this::mapToServiceDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BackendServiceDto createService(BackendServiceDto dto) {
        if (serviceRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Backend service name already exists: " + dto.getName());
        }

        BackendService service = BackendService.builder()
                .name(dto.getName())
                .baseUrl(dto.getBaseUrl())
                .healthEndpoint(dto.getHealthEndpoint() != null ? dto.getHealthEndpoint() : "/actuator/health")
                .status(dto.getStatus() != null ? dto.getStatus() : ServiceStatus.HEALTHY)
                .description(dto.getDescription())
                .build();

        BackendService saved = serviceRepository.save(service);
        return mapToServiceDto(saved);
    }

    @Transactional(readOnly = true)
    public List<GatewayRouteDto> getAllRoutes() {
        return routeRepository.findAll().stream()
                .map(this::mapToRouteDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public GatewayRouteDto createRoute(GatewayRouteDto dto) {
        if (routeRepository.existsByRouteId(dto.getRouteId())) {
            throw new IllegalArgumentException("Route ID already exists: " + dto.getRouteId());
        }

        BackendService service = serviceRepository.findById(dto.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Backend service not found: " + dto.getServiceId()));

        GatewayRoute route = GatewayRoute.builder()
                .routeId(dto.getRouteId())
                .service(service)
                .pathPattern(dto.getPathPattern())
                .targetPathPrefix(dto.getTargetPathPrefix())
                .requiresAuth(dto.getRequiresAuth() != null ? dto.getRequiresAuth() : true)
                .allowedRoles(dto.getAllowedRoles())
                .rateLimitPerMin(dto.getRateLimitPerMin() != null ? dto.getRateLimitPerMin() : 100)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        GatewayRoute saved = routeRepository.save(route);
        refreshGatewayRoutes();
        return mapToRouteDto(saved);
    }

    @Transactional
    public void deleteRoute(Long id) {
        if (!routeRepository.existsById(id)) {
            throw new IllegalArgumentException("Route not found: " + id);
        }
        routeRepository.deleteById(id);
        refreshGatewayRoutes();
    }

    public void refreshGatewayRoutes() {
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }

    private BackendServiceDto mapToServiceDto(BackendService service) {
        return BackendServiceDto.builder()
                .id(service.getId())
                .name(service.getName())
                .baseUrl(service.getBaseUrl())
                .healthEndpoint(service.getHealthEndpoint())
                .status(service.getStatus())
                .description(service.getDescription())
                .createdAt(service.getCreatedAt())
                .build();
    }

    private GatewayRouteDto mapToRouteDto(GatewayRoute route) {
        return GatewayRouteDto.builder()
                .id(route.getId())
                .routeId(route.getRouteId())
                .serviceId(route.getService().getId())
                .serviceName(route.getService().getName())
                .serviceBaseUrl(route.getService().getBaseUrl())
                .pathPattern(route.getPathPattern())
                .targetPathPrefix(route.getTargetPathPrefix())
                .requiresAuth(route.getRequiresAuth())
                .allowedRoles(route.getAllowedRoles())
                .rateLimitPerMin(route.getRateLimitPerMin())
                .isActive(route.getIsActive())
                .createdAt(route.getCreatedAt())
                .build();
    }
}
