package com.ahs.cvm.persistence.alert;

import com.ahs.cvm.domain.enums.AlertSeverity;
import com.ahs.cvm.domain.enums.AlertTriggerArt;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Konfigurierte Alert-Regel. */
@Entity
@Table(name = "alert_rule")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AlertRule {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Iteration 62D (CVM-62): Mandanten-Zuordnung. */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_art", nullable = false)
    private AlertTriggerArt triggerArt;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AlertSeverity severity;

    @Column(name = "cooldown_minutes", nullable = false)
    private Integer cooldownMinutes;

    @Column(name = "subject_prefix", nullable = false)
    private String subjectPrefix;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recipients", nullable = false, columnDefinition = "jsonb")
    private List<String> recipients;

    @Column(name = "condition_json")
    private String conditionJson;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void initialisiere() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (enabled == null) {
            enabled = Boolean.TRUE;
        }
        if (cooldownMinutes == null) {
            cooldownMinutes = 60;
        }
        if (subjectPrefix == null) {
            subjectPrefix = "[CVM]";
        }
    }

    @PreUpdate
    void aktualisiereZeitstempel() {
        updatedAt = Instant.now();
    }
}
