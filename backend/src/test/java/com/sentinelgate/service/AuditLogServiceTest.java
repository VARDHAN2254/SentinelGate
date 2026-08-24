package com.sentinelgate.service;

import com.sentinelgate.domain.AuditLog;
import com.sentinelgate.dto.AuditLogDto;
import com.sentinelgate.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        lenient().when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Should successfully record audit log entry")
    void recordAudit_Success() {
        AuditLog auditLog = auditLogService.recordAudit(
                "admin",
                "API_KEY_CREATED",
                "sg_live_a1b2c3d4",
                "Generated new API Key for payment microservice",
                "192.168.1.10"
        );

        assertNotNull(auditLog);
        assertEquals("admin", auditLog.getActorUsername());
        assertEquals("API_KEY_CREATED", auditLog.getAction());
        assertEquals("sg_live_a1b2c3d4", auditLog.getResource());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should retrieve paginated audit log entries")
    void getAuditLogs_Paginated() {
        AuditLog entry = AuditLog.builder()
                .id(1L)
                .actorUsername("secadmin")
                .action("LOGIN")
                .resource("SYSTEM")
                .build();

        Page<AuditLog> page = new PageImpl<>(List.of(entry));
        when(auditLogRepository.findAllByOrderByTimestampDesc(any(PageRequest.class))).thenReturn(page);

        Page<AuditLogDto> result = auditLogService.getAuditLogs(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("secadmin", result.getContent().get(0).getActorUsername());
    }
}
