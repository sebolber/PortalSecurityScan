package com.ahs.cvm.persistence.summary;

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
 * Persistierter Delta-Summary-Eintrag (Go-Live-Nachzug zu
 * Iteration 14, CVM-33).
 */
@Entity
@Table(name = "scan_delta_summary")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScanDeltaSummaryEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "scan_id", nullable = false, updatable = false)
    private UUID scanId;

    @Column(name = "previous_scan_id", updatable = false)
    private UUID previousScanId;

    @Column(name = "short_text", nullable = false)
    private String shortText;

    @Column(name = "long_text", nullable = false)
    private String longText;

    @Column(name = "llm_aufgerufen", nullable = false)
    private boolean llmAufgerufen;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void init() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
