package com.ahs.cvm.persistence.rulesuggestion;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.RuleSuggestionStatus;
import com.ahs.cvm.persistence.ai.AiSuggestion;
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
 * Extrahierter Regel-Vorschlag (Iteration 17, CVM-42). Referenziert
 * den {@link AiSuggestion}-Eintrag, der den LLM-Call dahinter audittiert.
 */
@Entity
@Table(name = "rule_suggestion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RuleSuggestion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_suggestion_id", nullable = false, updatable = false)
    private AiSuggestion aiSuggestion;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "condition_json", nullable = false, columnDefinition = "text")
    private String conditionJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposed_severity", nullable = false)
    private AhsSeverity proposedSeverity;

    @Column(name = "rationale_template", nullable = false)
    private String rationaleTemplate;

    @Column(name = "cluster_rationale", columnDefinition = "text")
    private String clusterRationale;

    @Column(name = "historical_match_count", nullable = false)
    private Integer historicalMatchCount;

    @Column(name = "would_have_covered", nullable = false)
    private Integer wouldHaveCovered;

    @Column(name = "coverage_rate", precision = 4, scale = 3, nullable = false)
    private BigDecimal coverageRate;

    @Column(name = "conflict_count", nullable = false)
    private Integer conflictCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RuleSuggestionStatus status;

    @Column(name = "suggested_by", nullable = false)
    private String suggestedBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_by")
    private String rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "reject_comment", columnDefinition = "text")
    private String rejectComment;

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
        if (status == null) {
            status = RuleSuggestionStatus.PROPOSED;
        }
        if (historicalMatchCount == null) {
            historicalMatchCount = 0;
        }
        if (wouldHaveCovered == null) {
            wouldHaveCovered = 0;
        }
        if (coverageRate == null) {
            coverageRate = BigDecimal.ZERO;
        }
        if (conflictCount == null) {
            conflictCount = 0;
        }
    }

    @PreUpdate
    void aktualisiereZeitstempel() {
        updatedAt = Instant.now();
    }
}
