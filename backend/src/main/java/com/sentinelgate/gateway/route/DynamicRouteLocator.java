package com.sentinelgate.gateway.route;

import com.sentinelgate.domain.GatewayRoute;
import com.sentinelgate.repository.GatewayRouteRepository;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
public class DynamicRouteLocator implements RouteLocator {

    private final RouteLocatorBuilder builder;
    private final GatewayRouteRepository routeRepository;

    public DynamicRouteLocator(RouteLocatorBuilder builder, GatewayRouteRepository routeRepository) {
        this.builder = builder;
        this.routeRepository = routeRepository;
    }

    @Override
    public Flux<Route> getRoutes() {
        RouteLocatorBuilder.Builder routesBuilder = builder.routes();
        List<GatewayRoute> activeRoutes = routeRepository.findByIsActiveTrue();

        for (GatewayRoute route : activeRoutes) {
            routesBuilder.route(route.getRouteId(), p -> 
                    p.path(route.getPathPattern()).uri(route.getService().getBaseUrl()));
        }

        return routesBuilder.build().getRoutes();
    }
}
