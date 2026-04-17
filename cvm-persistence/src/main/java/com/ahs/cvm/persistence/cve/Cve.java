package com.ahs.cvm.persistence.cve;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cve")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Cve {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cve_id", nullable = false, unique = true)
    private String cveId;

    @Column(name = "summary")
    private String summary;

    @Column(name = "cvss_base_score", precision = 3, scale = 1)
    private BigDecimal cvssBaseScore;

    @Column(name = "cvss_vector")
    private String cvssVector;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_modified_at")
    private Instant lastModifiedAt;

    @Column(name = "kev_listed", nullable = false)
    private Boolean kevListed;

    @Column(name = "kev_listed_at")
    private Instant kevListedAt;

    @Column(name = "epss_score", precision = 5, scale = 4)
    private BigDecimal epssScore;

    @Column(name = "epss_percentile", precision = 5, scale = 4)
    private BigDecimal epssPercentile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cwes", columnDefinition = "jsonb")
    private List<String> cwes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "advisories", columnDefinition = "jsonb")
    private List<Map<String, Object>> advisories;

    @Column(name = "last_fetched_at")
    private Instant lastFetchedAt;

    @Column(name = "source", nullable = false)
    private String source;

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
        if (kevListed == null) {
            kevListed = Boolean.FALSE;
        }
    }

    @PreUpdate
    void aktualisiereZeitstempel() {
        updatedAt = Instant.now();
    }
}
