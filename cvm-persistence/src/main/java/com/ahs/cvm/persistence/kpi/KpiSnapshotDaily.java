package com.ahs.cvm.persistence.kpi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kpi_snapshot_daily")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KpiSnapshotDaily {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "snapshot_day", nullable = false)
    private LocalDate snapshotDay;

    @Column(name = "product_version_id")
    private UUID productVersionId;

    @Column(name = "environment_id")
    private UUID environmentId;

    @Column(name = "open_critical", nullable = false)
    private int openCritical;

    @Column(name = "open_high", nullable = false)
    private int openHigh;

    @Column(name = "open_medium", nullable = false)
    private int openMedium;

    @Column(name = "open_low", nullable = false)
    private int openLow;

    @Column(name = "open_informational", nullable = false)
    private int openInformational;

    @Column(name = "automation_rate", nullable = false)
    private BigDecimal automationRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void initialisiere() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (automationRate == null) {
            automationRate = BigDecimal.ZERO;
        }
    }
}
