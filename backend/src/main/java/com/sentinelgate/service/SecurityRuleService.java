package com.sentinelgate.service;

import com.sentinelgate.domain.SecurityRule;
import com.sentinelgate.dto.SecurityRuleDto;
import com.sentinelgate.repository.SecurityRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SecurityRuleService {

    private final SecurityRuleRepository ruleRepository;
    private final AuditLogService auditLogService;

    public SecurityRuleService(SecurityRuleRepository ruleRepository, AuditLogService auditLogService) {
        this.ruleRepository = ruleRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<SecurityRuleDto> getAllRules() {
        return ruleRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SecurityRuleDto createRule(SecurityRuleDto dto, String actorUsername) {
        if (ruleRepository.existsByRuleName(dto.getRuleName())) {
            throw new IllegalArgumentException("Security rule name already exists: " + dto.getRuleName());
        }

        SecurityRule rule = SecurityRule.builder()
                .ruleName(dto.getRuleName())
                .ruleType(dto.getRuleType())
                .thresholdCount(dto.getThresholdCount())
                .windowSeconds(dto.getWindowSeconds())
                .actionTaken(dto.getActionTaken())
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .build();

        SecurityRule saved = ruleRepository.save(rule);

        auditLogService.recordAudit(
                actorUsername,
                "SECURITY_RULE_CREATED",
                saved.getRuleName(),
                "Created rule " + saved.getRuleName() + " with threshold " + saved.getThresholdCount(),
                "127.0.0.1"
        );

        return mapToDto(saved);
    }

    private SecurityRuleDto mapToDto(SecurityRule rule) {
        return SecurityRuleDto.builder()
                .id(rule.getId())
                .ruleName(rule.getRuleName())
                .ruleType(rule.getRuleType())
                .thresholdCount(rule.getThresholdCount())
                .windowSeconds(rule.getWindowSeconds())
                .actionTaken(rule.getActionTaken())
                .enabled(rule.getEnabled())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}
