package com.sentinelgate.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "c2VudGluZWxnYXRlX3NlY3VyZV9hcGlfZ2F0ZXdheV9zdXBlcl9zZWNyZXRfa2V5X2Zvcl9qc3RfMmU4YjRhOTM2YzE1ZmQ0ZWNmOGE3ZDI5YjM0MTY3Y2Q=";
    private final long expirationMs = 3600000; // 1 hour
    private final long refreshExpirationMs = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMs, refreshExpirationMs);
    }

    @Test
    @DisplayName("Should generate valid JWT token with username and role")
    void generateToken_Success() {
        String token = jwtTokenProvider.generateToken("security_admin", "ADMIN");

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("security_admin", jwtTokenProvider.getUsernameFromToken(token));
        assertEquals("ADMIN", jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    @DisplayName("Should generate valid Refresh Token")
    void generateRefreshToken_Success() {
        String refreshToken = jwtTokenProvider.generateRefreshToken("security_admin");

        assertNotNull(refreshToken);
        assertTrue(jwtTokenProvider.validateToken(refreshToken));
        assertEquals("security_admin", jwtTokenProvider.getUsernameFromToken(refreshToken));
    }

    @Test
    @DisplayName("Should reject tampered or invalid JWT token")
    void validateToken_InvalidToken() {
        String invalidToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJmYWtlIn0.invalid_signature";
        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }

    @Test
    @DisplayName("Should reject expired JWT token")
    void validateToken_ExpiredToken() {
        // Create token provider with 1ms expiration
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(secret, 1, 1);
        String token = shortLivedProvider.generateToken("test_user", "VIEWER");

        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {}

        assertFalse(shortLivedProvider.validateToken(token));
    }
}
