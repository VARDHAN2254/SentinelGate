package com.sentinelgate.gateway.filter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Component
public class LoggingAuditFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingAuditFilter.class);
    private static final String START_TIME_ATTR = "startTime";
    private final MeterRegistry meterRegistry;

    public LoggingAuditFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME_ATTR);
            long duration = startTime != null ? System.currentTimeMillis() - startTime : -1;

            String routeId = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR) != null ?
                    exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR).toString() : "UNKNOWN";

            String method = exchange.getRequest().getMethod().name();
            String path = exchange.getRequest().getURI().getPath();
            HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
            int status = statusCode != null ? statusCode.value() : 500;
            
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            String clientIp = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "UNKNOWN";

            // Record Micrometer metric counter
            Counter.builder("sentinelgate.requests.total")
                    .tag("method", method)
                    .tag("status", String.valueOf(status))
                    .tag("route", routeId)
                    .register(meterRegistry)
                    .increment();

            log.info("[GATEWAY TRAFFIC] method={} path={} status={} clientIp={} routeId={} durationMs={}",
                    method, path, status, clientIp, routeId, duration);
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
