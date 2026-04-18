package com.ahs.cvm.persistence.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cooldown-Tracking pro Regel und {@code triggerKey}. Existiert nur,
 * solange die Regel ueberhaupt schon einmal gefeuert hat.
 */
@Entity
@Table(
        name = "alert_event",
        uniqueConstraints =
                @UniqueConstraint(name = "uq_alert_event_rule_key", columnNames = {"rule_id", "trigger_key"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AlertEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "trigger_key", nullable = false)
    private String triggerKey;

    @Column(name = "last_fired_at", nullable = false)
    private Instant lastFiredAt;

    @Column(name = "suppressed_count", nullable = false)
    private Integer suppressedCount;

    @PrePersist
    void initialisiere() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (suppressedCount == null) {
            suppressedCount = 0;
        }
    }
}
