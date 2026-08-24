package com.sentinelgate.repository;

import com.sentinelgate.domain.ApiKey;
import com.sentinelgate.domain.enums.ApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyPrefix(String keyPrefix);
    boolean existsByKeyPrefix(String keyPrefix);
    long countByStatus(ApiKeyStatus status);
}
