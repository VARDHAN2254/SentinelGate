package com.sentinelgate.repository;

import com.sentinelgate.domain.SecurityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecurityRuleRepository extends JpaRepository<SecurityRule, Long> {
    Optional<SecurityRule> findByRuleName(String ruleName);
    boolean existsByRuleName(String ruleName);
}
