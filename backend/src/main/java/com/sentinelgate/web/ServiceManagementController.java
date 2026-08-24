package com.sentinelgate.web;

import com.sentinelgate.dto.BackendServiceDto;
import com.sentinelgate.service.GatewayRouteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/services")
public class ServiceManagementController {

    private final GatewayRouteService routeService;

    public ServiceManagementController(GatewayRouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public Mono<ResponseEntity<List<BackendServiceDto>>> getAllServices() {
        return Mono.fromCallable(routeService::getAllServices)
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> createService(@RequestBody BackendServiceDto dto) {
        return Mono.fromCallable(() -> routeService.createService(dto))
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).<Object>body(saved))
                .onErrorResume(IllegalArgumentException.class, ex -> 
                        Mono.just(ResponseEntity.badRequest().<Object>body(Map.of("error", ex.getMessage()))));
    }
}
