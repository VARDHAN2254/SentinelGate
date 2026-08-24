package com.sentinelgate.service;

import com.sentinelgate.domain.ApiKey;
import com.sentinelgate.domain.User;
import com.sentinelgate.domain.enums.ApiKeyStatus;
import com.sentinelgate.dto.ApiKeyDto;
import com.sentinelgate.dto.CreateApiKeyRequest;
import com.sentinelgate.dto.CreateApiKeyResponse;
import com.sentinelgate.repository.ApiKeyRepository;
import com.sentinelgate.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CreateApiKeyResponse generateApiKey(CreateApiKeyRequest request, String ownerUsername) {
        User owner = null;
        if (ownerUsername != null) {
            owner = userRepository.findByUsername(ownerUsername).orElse(null);
        }

        // Generate 4 bytes (8 hex chars) for prefix and 12 bytes (24 hex chars) for secret
        byte[] prefixBytes = new byte[4];
        byte[] secretBytes = new byte[12];
        secureRandom.nextBytes(prefixBytes);
        secureRandom.nextBytes(secretBytes);

        String prefixPart = HexFormat.of().formatHex(prefixBytes);
        String secretPart = HexFormat.of().formatHex(secretBytes);

        String keyPrefix = "sg_live_" + prefixPart; // Total 16 chars e.g. "sg_live_a1b2c3d4"
        String rawKey = keyPrefix + "_" + secretPart; // Full key e.g. "sg_live_a1b2c3d4_e5f6a7b8c9d0e1f2a3b4c5d6"

        String keyHash = passwordEncoder.encode(rawKey);

        ApiKey apiKey = ApiKey.builder()
                .name(request.getName() != null ? request.getName() : "API Key " + prefixPart)
                .keyPrefix(keyPrefix)
                .keyHash(keyHash)
                .clientOwner(owner)
                .status(ApiKeyStatus.ACTIVE)
                .rateLimitPerMin(request.getRateLimitPerMin() != null ? request.getRateLimitPerMin() : 1000)
                .expiresAt(request.getExpiresAt())
                .build();

        ApiKey saved = apiKeyRepository.save(apiKey);

        return CreateApiKeyResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .keyPrefix(saved.getKeyPrefix())
                .rawKey(rawKey) // RETURNED ONCE
                .status(saved.getStatus())
                .rateLimitPerMin(saved.getRateLimitPerMin())
                .expiresAt(saved.getExpiresAt())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ApiKeyDto> getAllApiKeys() {
        return apiKeyRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiKeyDto revokeApiKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));

        apiKey.setStatus(ApiKeyStatus.REVOKED);
        ApiKey saved = apiKeyRepository.save(apiKey);
        return mapToDto(saved);
    }

    private ApiKeyDto mapToDto(ApiKey key) {
        return ApiKeyDto.builder()
                .id(key.getId())
                .name(key.getName())
                .keyPrefix(key.getKeyPrefix())
                .ownerUsername(key.getClientOwner() != null ? key.getClientOwner().getUsername() : "SYSTEM")
                .status(key.getStatus())
                .rateLimitPerMin(key.getRateLimitPerMin())
                .expiresAt(key.getExpiresAt())
                .lastUsedAt(key.getLastUsedAt())
                .createdAt(key.getCreatedAt())
                .build();
    }
}
