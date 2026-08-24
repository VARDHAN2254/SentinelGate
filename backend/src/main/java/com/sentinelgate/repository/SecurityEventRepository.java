package com.sentinelgate.repository;

import com.sentinelgate.domain.SecurityEvent;
import com.sentinelgate.domain.enums.EventType;
import com.sentinelgate.domain.enums.SeverityLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    Page<SecurityEvent> findAllByOrderByTimestampDesc(Pageable pageable);
    List<SecurityEvent> findBySourceIpOrderByTimestampDesc(String sourceIp);
    List<SecurityEvent> findByEventTypeOrderByTimestampDesc(EventType eventType);
    List<SecurityEvent> findBySeverityOrderByTimestampDesc(SeverityLevel severity);
    long countByEventType(EventType eventType);
    long countByTimestampAfter(Instant timestamp);
    long countByTimestampBetween(Instant from, Instant to);
    long countByEventTypeAndTimestampBetween(EventType eventType, Instant from, Instant to);
    Page<SecurityEvent> findBySeverityOrderByTimestampDesc(SeverityLevel severity, Pageable pageable);
    Page<SecurityEvent> findByEventTypeOrderByTimestampDesc(EventType eventType, Pageable pageable);
}
