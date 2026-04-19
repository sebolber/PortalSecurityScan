package com.ahs.cvm.persistence.parameter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Unveraenderlicher Audit-Eintrag fuer jede Wertaenderung an einem
 * {@link SystemParameter}.
 *
 * <p>Der Service schreibt einen Eintrag bei: Anlage (alterWert=null),
 * Wertaenderung, Reset auf Default und Loeschung (neuerWert=null).
 * Sensitive Werte werden vor dem Schreiben durch {@code ***}
 * maskiert (DSGVO/CVM-Logging-Richtlinie).
 */
@Entity
@Table(name = "system_parameter_audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SystemParameterAuditLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "parameter_id", nullable = false, updatable = false)
    private UUID parameterId;

    @Column(name = "param_key", nullable = false, length = 255, updatable = false)
    private String paramKey;

    @Column(name = "old_value", columnDefinition = "text", updatable = false)
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text", updatable = false)
    private String newValue;

    @Column(name = "changed_by", nullable = false, length = 255, updatable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(name = "reason", columnDefinition = "text", updatable = false)
    private String reason;

    @PrePersist
    void initialisiere() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
