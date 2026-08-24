package com.sentinelgate.config;

import com.sentinelgate.domain.BackendService;
import com.sentinelgate.domain.GatewayRoute;
import com.sentinelgate.domain.Role;
import com.sentinelgate.domain.SecurityEvent;
import com.sentinelgate.domain.User;
import com.sentinelgate.domain.enums.EventType;
import com.sentinelgate.domain.enums.RoleType;
import com.sentinelgate.domain.enums.ServiceStatus;
import com.sentinelgate.domain.enums.SeverityLevel;
import com.sentinelgate.repository.BackendServiceRepository;
import com.sentinelgate.repository.GatewayRouteRepository;
import com.sentinelgate.repository.RoleRepository;
import com.sentinelgate.repository.SecurityEventRepository;
import com.sentinelgate.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Seeds the database with roles, a default admin user, sample gateway routes,
 * and — when {@code sentinelgate.demo.seed-events=true} — a set of realistic
 * demo security events that make the dashboard meaningful on first run.
 *
 * <p>Demo events are clearly labelled with {@code [DEMO]} in their description
 * so they can be distinguished from real production telemetry.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BackendServiceRepository serviceRepository;
    private final GatewayRouteRepository routeRepository;
    private final SecurityEventRepository securityEventRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${sentinelgate.admin.username:admin}")
    private String adminUsername;

    @Value("${sentinelgate.admin.password:AdminSecret123!}")
    private String adminPassword;

    @Value("${sentinelgate.demo.seed-events:true}")
    private boolean seedDemoEvents;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           BackendServiceRepository serviceRepository,
                           GatewayRouteRepository routeRepository,
                           SecurityEventRepository securityEventRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.routeRepository = routeRepository;
        this.securityEventRepository = securityEventRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
        seedSampleRoutes();
        if (seedDemoEvents && securityEventRepository.count() == 0) {
            seedDemoSecurityEvents();
        }
    }

    private void seedRoles() {
        Arrays.stream(RoleType.values()).forEach(roleType -> {
            if (roleRepository.findByName(roleType).isEmpty()) {
                roleRepository.save(Role.builder()
                        .name(roleType)
                        .description("System role: " + roleType.name())
                        .build());
            }
        });
    }

    private void seedAdminUser() {
        if (!userRepository.existsByUsername(adminUsername)) {
            Role adminRole = roleRepository.findByName(RoleType.ADMIN).orElseThrow();
            userRepository.save(User.builder()
                    .username(adminUsername)
                    .email(adminUsername + "@sentinelgate.io")
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .enabled(true)
                    .role(adminRole)
                    .build());
            log.info("Seeded admin user: {}", adminUsername);
        }
    }

    private void seedSampleRoutes() {
        if (!serviceRepository.existsByName("User Service")) {
            BackendService userService = serviceRepository.save(BackendService.builder()
                    .name("User Service")
                    .baseUrl("http://httpbin.org")
                    .healthEndpoint("/get")
                    .status(ServiceStatus.HEALTHY)
                    .description("Core identity and account microservice")
                    .build());
            routeRepository.save(GatewayRoute.builder()
                    .routeId("user_service_route")
                    .service(userService)
                    .pathPattern("/api/v1/users/**")
                    .requiresAuth(true)
                    .allowedRoles("ADMIN,DEVELOPER")
                    .rateLimitPerMin(100)
                    .isActive(true)
                    .build());
        }

        if (!serviceRepository.existsByName("Order Service")) {
            BackendService orderService = serviceRepository.save(BackendService.builder()
                    .name("Order Service")
                    .baseUrl("http://httpbin.org")
                    .healthEndpoint("/get")
                    .status(ServiceStatus.HEALTHY)
                    .description("E-commerce order processing microservice")
                    .build());
            routeRepository.save(GatewayRoute.builder()
                    .routeId("order_service_route")
                    .service(orderService)
                    .pathPattern("/api/v1/orders/**")
                    .requiresAuth(true)
                    .allowedRoles("ADMIN,DEVELOPER,VIEWER")
                    .rateLimitPerMin(200)
                    .isActive(true)
                    .build());
        }
    }

    /**
     * Seeds realistic demo security events so the dashboard is populated on first run.
     * Events span the last 2 hours with varied IPs, methods, and severity levels.
     * All descriptions are prefixed with [DEMO] for traceability.
     */
    private void seedDemoSecurityEvents() {
        Instant now = Instant.now();

        List<SecurityEvent> demoEvents = List.of(
            // Auth failures from various IPs — simulates credential stuffing
            buildEvent(EventType.AUTH_FAILURE, SeverityLevel.MEDIUM, "203.0.113.42",
                    "unknown_user", "/api/v1/auth/login", "POST",
                    "[DEMO] Authentication failed for user: unknown_user",
                    "AUTH_FAILURE_RULE", "LOG_AND_COUNT", now.minus(105, ChronoUnit.MINUTES)),
            buildEvent(EventType.AUTH_FAILURE, SeverityLevel.MEDIUM, "203.0.113.42",
                    "admin", "/api/v1/auth/login", "POST",
                    "[DEMO] Authentication failed for user: admin",
                    "AUTH_FAILURE_RULE", "LOG_AND_COUNT", now.minus(102, ChronoUnit.MINUTES)),
            buildEvent(EventType.AUTH_FAILURE, SeverityLevel.MEDIUM, "203.0.113.42",
                    "admin", "/api/v1/auth/login", "POST",
                    "[DEMO] Authentication failed for user: admin",
                    "AUTH_FAILURE_RULE", "LOG_AND_COUNT", now.minus(99, ChronoUnit.MINUTES)),
            buildEvent(EventType.AUTH_FAILURE, SeverityLevel.MEDIUM, "203.0.113.42",
                    "admin", "/api/v1/auth/login", "POST",
                    "[DEMO] Authentication failed for user: admin",
                    "AUTH_FAILURE_RULE", "LOG_AND_COUNT", now.minus(96, ChronoUnit.MINUTES)),
            buildEvent(EventType.AUTH_FAILURE, SeverityLevel.MEDIUM, "203.0.113.42",
                    "admin", "/api/v1/auth/login", "POST",
                    "[DEMO] Authentication failed for user: admin",
                    "AUTH_FAILURE_RULE", "LOG_AND_COUNT", now.minus(93, ChronoUnit.MINUTES)),

            // Brute-force threshold reached after 5 failures from same IP
            buildEvent(EventType.BRUTE_FORCE, SeverityLevel.HIGH, "203.0.113.42",
                    "admin", "/api/v1/auth/login", "POST",
                    "[DEMO] Brute-force login pattern detected from 203.0.113.42 (5 failures in 300s)",
                    "BRUTE_FORCE_RULE", "BLOCK_TEMPORARY", now.minus(93, ChronoUnit.MINUTES)),

            // Rate limit violations from a different attacker
            buildEvent(EventType.RATE_LIMIT_EXCEEDED, SeverityLevel.MEDIUM, "198.51.100.17",
                    null, "/api/v1/users/profile", "GET",
                    "[DEMO] Rate limit breached: 101/100 req/min from ip:198.51.100.17",
                    "RATE_LIMIT_RULE", "HTTP_429_TOO_MANY_REQUESTS", now.minus(78, ChronoUnit.MINUTES)),
            buildEvent(EventType.RATE_LIMIT_EXCEEDED, SeverityLevel.MEDIUM, "198.51.100.17",
                    null, "/api/v1/orders/recent", "GET",
                    "[DEMO] Rate limit breached: 145/100 req/min from ip:198.51.100.17",
                    "RATE_LIMIT_RULE", "HTTP_429_TOO_MANY_REQUESTS", now.minus(74, ChronoUnit.MINUTES)),

            // Suspicious IP after repeated violations
            buildEvent(EventType.SUSPICIOUS_IP, SeverityLevel.HIGH, "198.51.100.17",
                    null, "/api/v1/users/profile", "GET",
                    "[DEMO] Repeated rate-limit violations from 198.51.100.17 — flagged as suspicious",
                    "SUSPICIOUS_IP_RULE", "LOG_AND_FLAG", now.minus(72, ChronoUnit.MINUTES)),

            // Auth failures from a third source
            buildEvent(EventType.AUTH_FAILURE, SeverityLevel.MEDIUM, "192.0.2.88",
                    "developer1", "/api/v1/auth/login", "POST",
                    "[DEMO] Authentication failed for user: developer1",
                    "AUTH_FAILURE_RULE", "LOG_AND_COUNT", now.minus(45, ChronoUnit.MINUTES)),

            // Rate limit on authenticated user
            buildEvent(EventType.RATE_LIMIT_EXCEEDED, SeverityLevel.MEDIUM, "10.0.0.15",
                    "service-account-ci", "/api/v1/orders/batch", "POST",
                    "[DEMO] Rate limit breached: 305/300 req/min for user:service-account-ci",
                    "RATE_LIMIT_RULE", "HTTP_429_TOO_MANY_REQUESTS", now.minus(22, ChronoUnit.MINUTES)),

            // Recent auth failure
            buildEvent(EventType.AUTH_FAILURE, SeverityLevel.MEDIUM, "203.0.113.91",
                    "root", "/api/v1/auth/login", "POST",
                    "[DEMO] Authentication failed for user: root",
                    "AUTH_FAILURE_RULE", "LOG_AND_COUNT", now.minus(8, ChronoUnit.MINUTES))
        );

        securityEventRepository.saveAll(demoEvents);
        log.info("Seeded {} demo security events for dashboard visualisation", demoEvents.size());
    }

    private SecurityEvent buildEvent(EventType type, SeverityLevel severity, String sourceIp,
                                      String clientIdentity, String endpoint, String method,
                                      String description, String rule, String action, Instant timestamp) {
        return SecurityEvent.builder()
                .eventUuid(UUID.randomUUID().toString())
                .eventType(type)
                .severity(severity)
                .sourceIp(sourceIp)
                .clientIdentity(clientIdentity)
                .endpoint(endpoint)
                .httpMethod(method)
                .description(description)
                .ruleTriggered(rule)
                .actionTaken(action)
                .timestamp(timestamp)
                .build();
    }
}
