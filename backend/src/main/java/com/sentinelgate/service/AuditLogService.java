package com.sentinelgate.service;

import com.sentinelgate.domain.AuditLog;
import com.sentinelgate.dto.AuditLogDto;
import com.sentinelgate.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog recordAudit(String actorUsername, String action, String resource, String details, String sourceIp) {
        AuditLog auditLog = AuditLog.builder()
                .actorUsername(actorUsername != null ? actorUsername : "SYSTEM")
                .action(action)
                .resource(resource)
                .details(details)
                .sourceIp(sourceIp != null ? sourceIp : "127.0.0.1")
                .timestamp(Instant.now())
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("[AUDIT TRAIL] actor={} action={} resource={}", actorUsername, action, resource);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size))
                .map(this::mapToDto);
    }

    private AuditLogDto mapToDto(AuditLog auditLog) {
        return AuditLogDto.builder()
                .id(auditLog.getId())
                .actorUsername(auditLog.getActorUsername())
                .action(auditLog.getAction())
                .resource(auditLog.getResource())
                .details(auditLog.getDetails())
                .sourceIp(auditLog.getSourceIp())
                .timestamp(auditLog.getTimestamp())
                .build();
    }
}
