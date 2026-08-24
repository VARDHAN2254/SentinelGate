package com.sentinelgate.domain;

import com.sentinelgate.domain.enums.EventType;
import com.sentinelgate.domain.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "security_events", indexes = {
        @Index(name = "idx_sec_events_timestamp", columnList = "timestamp"),
        @Index(name = "idx_sec_events_type_sev", columnList = "event_type, severity"),
        @Index(name = "idx_sec_events_ip", columnList = "source_ip")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_uuid", nullable = false, unique = true, length = 36)
    private String eventUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeverityLevel severity;

    @Column(name = "source_ip", nullable = false, length = 45)
    private String sourceIp;

    @Column(name = "client_identity", length = 100)
    private String clientIdentity;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rule_triggered", length = 100)
    private String ruleTriggered;

    @Column(name = "action_taken", length = 50)
    private String actionTaken;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant timestamp = Instant.now();
}
