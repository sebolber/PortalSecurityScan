package com.ahs.cvm.persistence.anomaly;

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
 * Eintrag der Anomalie-Erkennung (Iteration 18, CVM-43).
 * {@code pattern}-Werte entsprechen dem DB-Check-Constraint in
 * V0019. Die JPA-Felder sind bewusst {@code String} statt
 * Enum, damit neue Muster ohne Entity-Aenderung ergaenzt werden
 * koennen - Konsistenz wird ueber den DB-Constraint sichergestellt.
 */
@Entity
@Table(name = "anomaly_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AnomalyEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "assessment_id", nullable = false, updatable = false)
    private UUID assessmentId;

    @Column(name = "pattern", nullable = false, updatable = false)
    private String pattern;

    @Column(name = "severity", nullable = false, updatable = false)
    private String severity;

    @Column(name = "reason", nullable = false, updatable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "pointers_json", updatable = false, columnDefinition = "text")
    private String pointersJson;

    @Column(name = "triggered_at", nullable = false, updatable = false)
    private Instant triggeredAt;

    @PrePersist
    void init() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (triggeredAt == null) {
            triggeredAt = Instant.now();
        }
    }
}
