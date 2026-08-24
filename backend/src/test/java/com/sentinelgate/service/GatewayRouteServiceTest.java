package com.sentinelgate.service;

import com.sentinelgate.domain.BackendService;
import com.sentinelgate.domain.GatewayRoute;
import com.sentinelgate.domain.enums.ServiceStatus;
import com.sentinelgate.dto.BackendServiceDto;
import com.sentinelgate.dto.GatewayRouteDto;
import com.sentinelgate.repository.BackendServiceRepository;
import com.sentinelgate.repository.GatewayRouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayRouteServiceTest {

    @Mock
    private BackendServiceRepository serviceRepository;

    @Mock
    private GatewayRouteRepository routeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GatewayRouteService routeService;

    private BackendService sampleService;

    @BeforeEach
    void setUp() {
        sampleService = BackendService.builder()
                .id(1L)
                .name("Test Service")
                .baseUrl("http://localhost:8081")
                .healthEndpoint("/actuator/health")
                .status(ServiceStatus.HEALTHY)
                .build();
    }

    @Test
    @DisplayName("Should successfully create backend service")
    void createService_Success() {
        BackendServiceDto dto = BackendServiceDto.builder()
                .name("New Service")
                .baseUrl("http://localhost:8082")
                .build();

        when(serviceRepository.existsByName("New Service")).thenReturn(false);
        when(serviceRepository.save(any(BackendService.class))).thenAnswer(i -> {
            BackendService s = i.getArgument(0);
            s.setId(5L);
            return s;
        });

        BackendServiceDto response = routeService.createService(dto);

        assertNotNull(response);
        assertEquals("New Service", response.getName());
        verify(serviceRepository).save(any());
    }

    @Test
    @DisplayName("Should create route and trigger RefreshRoutesEvent")
    void createRoute_TriggersRefresh() {
        GatewayRouteDto dto = GatewayRouteDto.builder()
                .routeId("test_route")
                .serviceId(1L)
                .pathPattern("/api/v1/test/**")
                .build();

        when(routeRepository.existsByRouteId("test_route")).thenReturn(false);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(sampleService));
        when(routeRepository.save(any(GatewayRoute.class))).thenAnswer(i -> {
            GatewayRoute r = i.getArgument(0);
            r.setId(10L);
            return r;
        });

        GatewayRouteDto response = routeService.createRoute(dto);

        assertNotNull(response);
        assertEquals("test_route", response.getRouteId());
        verify(eventPublisher).publishEvent(any(RefreshRoutesEvent.class));
    }
}
