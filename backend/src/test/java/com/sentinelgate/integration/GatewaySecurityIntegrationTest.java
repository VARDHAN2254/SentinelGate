package com.sentinelgate.integration;

import com.sentinelgate.domain.ApiKey;
import com.sentinelgate.domain.enums.ApiKeyStatus;
import com.sentinelgate.domain.enums.RoleType;
import com.sentinelgate.dto.CreateApiKeyRequest;
import com.sentinelgate.dto.LoginRequest;
import com.sentinelgate.dto.RegisterRequest;
import com.sentinelgate.repository.ApiKeyRepository;
import com.sentinelgate.repository.SecurityEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests that start the full Spring application
 * against an in-memory H2 database.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                // Disable demo seeding in tests — tests generate their own events
                "sentinelgate.demo.seed-events=false"
        }
)
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class GatewaySecurityIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private SecurityEventRepository securityEventRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // -------------------------------------------------------------------------
    // Health
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Health: /api/v1/system/health returns UP")
    void healthCheck_returnsUp() {
        webTestClient.get()
                .uri("/api/v1/system/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.service").isEqualTo("SentinelGate API Gateway");
    }

    // -------------------------------------------------------------------------
    // Authentication — registration
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("Auth: Register valid user returns 201 Created")
    void register_validUser_returns201() {
        RegisterRequest req = new RegisterRequest("testuser_" + System.currentTimeMillis(),
                "test_" + System.currentTimeMillis() + "@sg.io", "SecurePass1!", RoleType.VIEWER);

        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.username").isEqualTo(req.getUsername())
                .jsonPath("$.role").isEqualTo("VIEWER");
    }

    @Test
    @Order(3)
    @DisplayName("Auth: Duplicate registration returns 409 Conflict")
    void register_duplicateUsername_returns409() {
        RegisterRequest req = new RegisterRequest("admin", "admin_dup@sg.io", "Pass1!", RoleType.VIEWER);

        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.error").exists();
    }

    // -------------------------------------------------------------------------
    // Authentication — login and JWT
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("Auth: Login with valid credentials returns JWT")
    void login_validCredentials_returnsJwt() {
        LoginRequest req = new LoginRequest("admin", "AdminSecret123!");

        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").exists()
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.role").isEqualTo("ADMIN");
    }

    /** Logs in as admin and extracts the raw JWT string for use in tests. */
    private String getAdminToken() {
        LoginRequest req = new LoginRequest("admin", "AdminSecret123!");
        byte[] body = webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBodyContent();

        if (body == null) return null;
        String bodyStr = new String(body);
        int start = bodyStr.indexOf("\"accessToken\":\"") + 15;
        int end = bodyStr.indexOf("\"", start);
        return (start > 14 && end > start) ? bodyStr.substring(start, end) : null;
    }

    /** Registers and logs in a regular VIEWER user, returning the JWT token. */
    private String getViewerToken() {
        String username = "viewer_" + System.currentTimeMillis();
        RegisterRequest reg = new RegisterRequest(username, username + "@sg.io", "ViewerPass1!", RoleType.VIEWER);
        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(reg)
                .exchange()
                .expectStatus().isCreated();

        LoginRequest login = new LoginRequest(username, "ViewerPass1!");
        byte[] body = webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(login)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBodyContent();

        if (body == null) return null;
        String bodyStr = new String(body);
        int start = bodyStr.indexOf("\"accessToken\":\"") + 15;
        int end = bodyStr.indexOf("\"", start);
        return (start > 14 && end > start) ? bodyStr.substring(start, end) : null;
    }

    @Test
    @Order(5)
    @DisplayName("Auth: Login with wrong password returns 401")
    void login_wrongPassword_returns401() {
        LoginRequest req = new LoginRequest("admin", "WrongPassword!");

        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").exists();
    }

    @Test
    @Order(6)
    @DisplayName("Auth: Login with non-existent user returns 401")
    void login_nonExistentUser_returns401() {
        LoginRequest req = new LoginRequest("nobody_exists_" + System.currentTimeMillis(), "Pass123!");

        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").exists();
    }

    @Test
    @Order(7)
    @DisplayName("Auth: /me with invalid JWT returns 401")
    void me_invalidJwt_returns401() {
        webTestClient.get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer this.is.not.a.valid.jwt")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Order(8)
    @DisplayName("Auth: /me with no Authorization header returns 401")
    void me_noToken_returns401() {
        webTestClient.get().uri("/api/v1/auth/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Order(9)
    @DisplayName("Auth: /me with valid admin token returns profile")
    void me_validAdminToken_returnsProfile() {
        String token = getAdminToken();
        assertThat(token).isNotNull();

        webTestClient.get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo("admin")
                .jsonPath("$.role").isEqualTo("ADMIN");
    }

    // -------------------------------------------------------------------------
    // Authorization & RBAC
    // -------------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("AuthZ: Admin endpoint without token returns 401")
    void adminEndpoint_noToken_returns401() {
        webTestClient.get().uri("/api/v1/admin/api-keys")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Order(11)
    @DisplayName("AuthZ: Admin endpoint with regular VIEWER token returns 403 Forbidden")
    void adminEndpoint_withViewerToken_returns403() {
        String viewerToken = getViewerToken();
        assertThat(viewerToken).isNotNull();

        webTestClient.get().uri("/api/v1/admin/api-keys")
                .header("Authorization", "Bearer " + viewerToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @Order(12)
    @DisplayName("AuthZ: Admin endpoint with admin token returns 200")
    void adminEndpoint_withAdminToken_returns200() {
        String token = getAdminToken();
        assertThat(token).isNotNull();

        webTestClient.get().uri("/api/v1/admin/api-keys")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
    }

    // -------------------------------------------------------------------------
    // Security detection — brute force
    // -------------------------------------------------------------------------

    @Test
    @Order(13)
    @DisplayName("Security: 5 failed logins generate AUTH_FAILURE security events in DB")
    void bruteForce_5FailedLogins_generatesAuthFailureEvents() {
        String targetUser = "target_user_" + System.currentTimeMillis();
        LoginRequest req = new LoginRequest(targetUser, "wrong_password");

        long eventsBefore = securityEventRepository.count();

        for (int i = 0; i < 5; i++) {
            webTestClient.post().uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(req)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        long eventsAfter = securityEventRepository.count();
        assertThat(eventsAfter - eventsBefore).isGreaterThanOrEqualTo(5);
    }

    // -------------------------------------------------------------------------
    // API Key lifecycle
    // -------------------------------------------------------------------------

    @Test
    @Order(14)
    @DisplayName("API Key: Create key returns sg_live_ prefixed raw key once")
    void apiKey_create_returnsRawKey() {
        String token = getAdminToken();
        assertThat(token).isNotNull();

        CreateApiKeyRequest req = new CreateApiKeyRequest("Test Integration Key", 100, null);

        webTestClient.post().uri("/api/v1/admin/api-keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.rawKey").exists()
                .jsonPath("$.keyPrefix").value(prefix -> assertThat(prefix.toString()).startsWith("sg_live_"));
    }

    @Test
    @Order(15)
    @DisplayName("API Key: Full lifecycle — create, revoke, rejected on reuse")
    void apiKey_fullLifecycle_createRevokeReject() {
        String token = getAdminToken();
        assertThat(token).isNotNull();

        // Step 1: Create a new API key
        CreateApiKeyRequest req = new CreateApiKeyRequest("Lifecycle Test Key", 50, null);
        byte[] createBody = webTestClient.post().uri("/api/v1/admin/api-keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult()
                .getResponseBodyContent();

        assertThat(createBody).isNotNull();
        String createJson = new String(createBody);

        int rawStart = createJson.indexOf("\"rawKey\":\"") + 10;
        int rawEnd = createJson.indexOf("\"", rawStart);
        assertThat(rawStart).isGreaterThan(9);
        String rawKey = createJson.substring(rawStart, rawEnd);
        assertThat(rawKey).startsWith("sg_live_");

        int idStart = createJson.indexOf("\"id\":") + 5;
        int idEnd = createJson.indexOf(",", idStart);
        long createdId = Long.parseLong(createJson.substring(idStart, idEnd).trim());

        // Step 2: Valid key works on overview
        webTestClient.get().uri("/api/v1/analytics/overview")
                .header("X-API-KEY", rawKey)
                .exchange()
                .expectStatus().isOk();

        // Step 3: Revoke the key
        webTestClient.post().uri("/api/v1/admin/api-keys/" + createdId + "/revoke")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("REVOKED");

        // Step 4: Attempting to use the revoked key must return 403
        webTestClient.get().uri("/api/v1/analytics/overview")
                .header("X-API-KEY", rawKey)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error").value(msg -> assertThat(msg.toString()).contains("revoked"));
    }

    @Test
    @Order(16)
    @DisplayName("API Key: Malformed key format returns 401 Unauthorized")
    void apiKey_malformed_returns401() {
        webTestClient.get().uri("/api/v1/analytics/overview")
                .header("X-API-KEY", "sg_live_short_bad_key")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Malformed API key format");
    }

    @Test
    @Order(17)
    @DisplayName("API Key: Non-existent key prefix returns 401 Unauthorized")
    void apiKey_unknown_returns401() {
        webTestClient.get().uri("/api/v1/analytics/overview")
                .header("X-API-KEY", "sg_live_00000000_111122223333444455556666")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Invalid API key");
    }

    @Test
    @Order(18)
    @DisplayName("API Key: Expired API key returns 403 Forbidden")
    void apiKey_expired_returns403() {
        String rawKey = "sg_live_exp00001_112233445566778899001122";
        String keyPrefix = rawKey.substring(0, 16);

        ApiKey expiredKey = ApiKey.builder()
                .name("Expired Test Key")
                .keyPrefix(keyPrefix)
                .keyHash(passwordEncoder.encode(rawKey))
                .status(ApiKeyStatus.ACTIVE)
                .rateLimitPerMin(100)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .createdAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .build();
        apiKeyRepository.save(expiredKey);

        webTestClient.get().uri("/api/v1/analytics/overview")
                .header("X-API-KEY", rawKey)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error").isEqualTo("API key has expired");
    }

    // -------------------------------------------------------------------------
    // Analytics
    // -------------------------------------------------------------------------

    @Test
    @Order(19)
    @DisplayName("Analytics: Overview endpoint returns real DB counts")
    void analytics_overview_returnsRealCounts() {
        webTestClient.get().uri("/api/v1/analytics/overview")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalSecurityEvents").isNumber()
                .jsonPath("$.authFailures").isNumber()
                .jsonPath("$.rateLimitHits").isNumber()
                .jsonPath("$.activeRoutes").isNumber();
    }

    @Test
    @Order(20)
    @DisplayName("Analytics: Traffic timeline returns 12 buckets")
    void analytics_timeline_returns12Buckets() {
        webTestClient.get().uri("/api/v1/analytics/traffic-timeline")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(12);
    }

    @Test
    @Order(21)
    @DisplayName("Analytics: Security events endpoint returns paginated results")
    void analytics_events_returnsPaginatedResults() {
        webTestClient.get().uri("/api/v1/analytics/events?page=0&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.pageable").exists();
    }

    @Test
    @Order(22)
    @DisplayName("Analytics: Filter events by severity")
    void analytics_events_filterBySeverity() {
        webTestClient.get().uri("/api/v1/analytics/events?page=0&size=10&severity=MEDIUM")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray();
    }

    @Test
    @Order(23)
    @DisplayName("Analytics: Filter events by eventType")
    void analytics_events_filterByEventType() {
        webTestClient.get().uri("/api/v1/analytics/events?page=0&size=10&eventType=AUTH_FAILURE")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray();
    }

    // -------------------------------------------------------------------------
    // Audit Logs & Security Rules Admin Endpoints
    // -------------------------------------------------------------------------

    @Test
    @Order(24)
    @DisplayName("Audit: Admin can view audit logs")
    void auditLogs_adminCanView() {
        String token = getAdminToken();
        assertThat(token).isNotNull();

        webTestClient.get().uri("/api/v1/admin/audit-logs")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray();
    }

    @Test
    @Order(25)
    @DisplayName("Rules: Admin can view security rules")
    void securityRules_adminCanView() {
        String token = getAdminToken();
        assertThat(token).isNotNull();

        webTestClient.get().uri("/api/v1/admin/security-rules")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();
    }

    @Test
    @Order(26)
    @DisplayName("Routes: Admin can view registered gateway routes")
    void gatewayRoutes_adminCanView() {
        String token = getAdminToken();
        assertThat(token).isNotNull();

        webTestClient.get().uri("/api/v1/admin/routes")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();
    }
}
