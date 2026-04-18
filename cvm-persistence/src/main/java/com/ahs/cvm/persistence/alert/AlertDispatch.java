package com.ahs.cvm.persistence.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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

/**
 * Audit-Eintrag pro tatsaechlich ausgeloestem Mail-Versand. Speichert
 * Empfaenger, Subject und einen Body-Auszug. Bei Fehlern (z.&nbsp;B.
 * SMTP-Auth) wird {@code error} gefuellt.
 */
@Entity
@Table(name = "alert_dispatch")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AlertDispatch {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "trigger_key", nullable = false)
    private String triggerKey;

    @Column(name = "dispatched_at", nullable = false)
    private Instant dispatchedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recipients", nullable = false, columnDefinition = "jsonb")
    private List<String> recipients;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "body_excerpt")
    private String bodyExcerpt;

    @Column(name = "dry_run", nullable = false)
    private Boolean dryRun;

    @Column(name = "error")
    private String error;

    @PrePersist
    void initialisiere() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (dryRun == null) {
            dryRun = Boolean.FALSE;
        }
    }
}
