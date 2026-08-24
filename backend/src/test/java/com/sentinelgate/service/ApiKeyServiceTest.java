package com.sentinelgate.service;

import com.sentinelgate.domain.ApiKey;
import com.sentinelgate.domain.enums.ApiKeyStatus;
import com.sentinelgate.dto.ApiKeyDto;
import com.sentinelgate.dto.CreateApiKeyRequest;
import com.sentinelgate.dto.CreateApiKeyResponse;
import com.sentinelgate.repository.ApiKeyRepository;
import com.sentinelgate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed_key_secret");
    }

    @Test
    @DisplayName("Should generate secure API key with prefix sg_live_ and hash secret")
    void generateApiKey_Success() {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Analytics Service Key")
                .rateLimitPerMin(5000)
                .build();

        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(i -> {
            ApiKey k = i.getArgument(0);
            k.setId(100L);
            return k;
        });

        CreateApiKeyResponse response = apiKeyService.generateApiKey(request, null);

        assertNotNull(response);
        assertNotNull(response.getRawKey());
        assertTrue(response.getRawKey().startsWith("sg_live_"));
        assertTrue(response.getKeyPrefix().startsWith("sg_live_"));
        assertEquals(16, response.getKeyPrefix().length());
        assertEquals("Analytics Service Key", response.getName());
        assertEquals(ApiKeyStatus.ACTIVE, response.getStatus());

        verify(passwordEncoder).encode(response.getRawKey());
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("Should successfully revoke active API key")
    void revokeApiKey_Success() {
        ApiKey apiKey = ApiKey.builder()
                .id(100L)
                .name("Revoke Test Key")
                .keyPrefix("sg_live_12345678")
                .status(ApiKeyStatus.ACTIVE)
                .build();

        when(apiKeyRepository.findById(100L)).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(i -> i.getArgument(0));

        ApiKeyDto response = apiKeyService.revokeApiKey(100L);

        assertNotNull(response);
        assertEquals(ApiKeyStatus.REVOKED, response.getStatus());
        verify(apiKeyRepository).save(apiKey);
    }
}
