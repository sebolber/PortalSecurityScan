package com.ahs.cvm.persistence.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
@Table(name = "rule_dry_run_result")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RuleDryRunResult {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "range_start", nullable = false)
    private Instant rangeStart;

    @Column(name = "range_end", nullable = false)
    private Instant rangeEnd;

    @Column(name = "total_findings", nullable = false)
    private Integer totalFindings;

    @Column(name = "matched_findings", nullable = false)
    private Integer matchedFindings;

    @Column(name = "matched_already_approved", nullable = false)
    private Integer matchedAlreadyApproved;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conflicts", columnDefinition = "jsonb")
    private List<Map<String, Object>> conflicts;

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
        if (executedAt == null) {
            executedAt = Instant.now();
        }
    }
}
