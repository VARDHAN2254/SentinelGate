package com.sentinelgate.web;

import com.sentinelgate.dto.GatewayRouteDto;
import com.sentinelgate.service.GatewayRouteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/routes")
public class RouteManagementController {

    private final GatewayRouteService routeService;

    public RouteManagementController(GatewayRouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public Mono<ResponseEntity<List<GatewayRouteDto>>> getAllRoutes() {
        return Mono.fromCallable(routeService::getAllRoutes)
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> createRoute(@RequestBody GatewayRouteDto dto) {
        return Mono.fromCallable(() -> routeService.createRoute(dto))
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).<Object>body(saved))
                .onErrorResume(IllegalArgumentException.class, ex -> 
                        Mono.just(ResponseEntity.badRequest().<Object>body(Map.of("error", ex.getMessage()))));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> deleteRoute(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
                    routeService.deleteRoute(id);
                    return ResponseEntity.noContent().build();
                })
                .onErrorResume(IllegalArgumentException.class, ex -> 
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).<Object>body(Map.of("error", ex.getMessage()))));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<Map<String, String>>> refreshRoutes() {
        return Mono.fromRunnable(routeService::refreshGatewayRoutes)
                .then(Mono.just(ResponseEntity.ok(Map.of("message", "Gateway routes refreshed successfully"))));
    }
}
