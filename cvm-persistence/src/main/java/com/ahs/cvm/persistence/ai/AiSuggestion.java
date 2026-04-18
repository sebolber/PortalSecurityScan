package com.ahs.cvm.persistence.ai;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AiSuggestionStatus;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.finding.Finding;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Strukturierter KI-Vorschlag. Referenziert immer einen
 * {@link AiCallAudit}. Der Status bleibt {@link AiSuggestionStatus#PROPOSED},
 * bis ein Mensch ueber die Assessment-Workflow-UI entscheidet (Konzept 4.4).
 */
@Entity
@Table(name = "ai_suggestion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AiSuggestion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_call_audit_id", nullable = false, updatable = false)
    private AiCallAudit aiCallAudit;

    @Column(name = "use_case", nullable = false, updatable = false)
    private String useCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id", updatable = false)
    private Finding finding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cve_id", updatable = false)
    private Cve cve;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", updatable = false)
    private Environment environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", updatable = false)
    private AhsSeverity severity;

    @Column(name = "rationale", updatable = false, columnDefinition = "text")
    private String rationale;

    @Column(name = "confidence", precision = 4, scale = 3, updatable = false)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AiSuggestionStatus status;

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
        if (status == null) {
            status = AiSuggestionStatus.PROPOSED;
        }
    }
}
