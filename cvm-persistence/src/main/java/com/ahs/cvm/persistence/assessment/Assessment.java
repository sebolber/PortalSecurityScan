package com.ahs.cvm.persistence.assessment;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.product.ProductVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Unveraenderliches, versioniertes Assessment. Siehe {@link AssessmentImmutabilityListener}.
 * Das Feld {@link #aiSuggestionId} bleibt in Iteration 01 ohne FK; Iteration 11
 * ergaenzt die Referenz-Tabelle und schaltet den FK scharf.
 */
@Entity
@Table(name = "assessment")
@EntityListeners(AssessmentImmutabilityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Assessment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finding_id", nullable = false, updatable = false)
    private Finding finding;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_version_id", nullable = false, updatable = false)
    private ProductVersion productVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "environment_id", nullable = false, updatable = false)
    private Environment environment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cve_id", nullable = false, updatable = false)
    private Cve cve;

    @Column(name = "version", nullable = false, updatable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AssessmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, updatable = false)
    private AhsSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposal_source", nullable = false, updatable = false)
    private ProposalSource proposalSource;

    @Column(name = "rationale", updatable = false)
    private String rationale;

    @Column(name = "decided_by", updatable = false)
    private String decidedBy;

    @Column(name = "decided_at", updatable = false)
    private Instant decidedAt;

    @Column(name = "superseded_at")
    private Instant supersededAt;

    @Column(name = "ai_suggestion_id", updatable = false)
    private UUID aiSuggestionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Markiert, dass der einzig erlaubte Update-Pfad &mdash; Setzen von
     * {@code supersededAt} &mdash; aktiv ist. Wird vom
     * {@link AssessmentImmutabilityListener} ausgewertet und von
     * {@code AssessmentLookupService#markAlsUeberholt(...)} gesetzt.
     */
    @Transient
    @Builder.Default
    private boolean supersedingAllowed = false;

    @PrePersist
    void initialisiere() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (version == null) {
            version = 1;
        }
    }

    public void markiereAlsUeberholt(Instant zeitpunkt) {
        this.supersedingAllowed = true;
        this.supersededAt = zeitpunkt;
        this.status = AssessmentStatus.SUPERSEDED;
    }
}
