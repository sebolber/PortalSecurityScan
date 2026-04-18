package com.ahs.cvm.persistence.waiver;

import com.ahs.cvm.domain.enums.WaiverStatus;
import com.ahs.cvm.persistence.assessment.Assessment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
 * Zeitlich befristete Risiko-Akzeptanz (Iteration 20, CVM-51).
 */
@Entity
@Table(name = "waiver")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Waiver {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false, updatable = false)
    private Assessment assessment;

    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "granted_by", nullable = false, updatable = false)
    private String grantedBy;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "review_interval_days", nullable = false)
    private Integer reviewIntervalDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WaiverStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "revoked_by")
    private String revokedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "extended_by")
    private String extendedBy;

    @Column(name = "extended_at")
    private Instant extendedAt;

    @PrePersist
    void init() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = WaiverStatus.ACTIVE;
        }
        if (reviewIntervalDays == null) {
            reviewIntervalDays = 90;
        }
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
