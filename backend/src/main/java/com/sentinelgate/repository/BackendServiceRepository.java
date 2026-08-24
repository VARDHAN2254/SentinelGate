package com.sentinelgate.repository;

import com.sentinelgate.domain.BackendService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BackendServiceRepository extends JpaRepository<BackendService, Long> {
    Optional<BackendService> findByName(String name);
    boolean existsByName(String name);
}
