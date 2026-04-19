package com.ahs.cvm.persistence.rule;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.RuleOrigin;
import com.ahs.cvm.domain.enums.RuleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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

@Entity
@Table(name = "rule")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Rule {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "rule_key", nullable = false, unique = true)
    private String ruleKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false)
    private RuleOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RuleStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposed_severity", nullable = false)
    private AhsSeverity proposedSeverity;

    @Column(name = "condition_json", nullable = false, columnDefinition = "text")
    private String conditionJson;

    @Column(name = "rationale_template", nullable = false)
    private String rationaleTemplate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rationale_source_fields", columnDefinition = "jsonb")
    private List<String> rationaleSourceFields;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "activated_by")
    private String activatedBy;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "retired_at")
    private Instant retiredAt;

    /**
     * Soft-Delete-Marker (Iteration 50, CVM-100). {@code null} = aktiv.
     * Abgrenzung zu {@link #retiredAt}:
     * <ul>
     *   <li>{@link #retiredAt} = fachlich abgeloest (Supersede-Kette,
     *       historische Audit-Pflicht);</li>
     *   <li>{@code deletedAt} = technisch entfernt (Admin-Cleanup);
     *       Regel-Engine beruecksichtigt sie nicht mehr.</li>
     * </ul>
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "extracted_from_ai_suggestion_id")
    private UUID extractedFromAiSuggestionId;

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
            status = RuleStatus.DRAFT;
        }
        if (origin == null) {
            origin = RuleOrigin.MANUAL;
        }
    }

    @PreUpdate
    void aktualisiereZeitstempel() {
        updatedAt = Instant.now();
    }
}
