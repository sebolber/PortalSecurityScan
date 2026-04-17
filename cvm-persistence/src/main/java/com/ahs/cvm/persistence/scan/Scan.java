package com.ahs.cvm.persistence.scan;

import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.product.ProductVersion;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "scan")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Scan {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_version_id", nullable = false)
    private ProductVersion productVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id")
    private Environment environment;

    @Column(name = "sbom_format", nullable = false)
    private String sbomFormat;

    @Column(name = "sbom_checksum", nullable = false)
    private String sbomChecksum;

    @Column(name = "content_sha256", nullable = false)
    private String contentSha256;

    /**
     * Verschluesselte SBOM-Rohdaten als BYTEA. {@code @Lob} wuerde Hibernate
     * 6 zu Postgres-OID/BLOB greifen lassen und gegen die Migration
     * (V0006: BYTEA) verstossen. Wir mappen daher explizit auf VARBINARY.
     */
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "raw_sbom")
    private byte[] rawSbom;

    @Column(name = "scanned_at", nullable = false)
    private Instant scannedAt;

    @Column(name = "scanner")
    private String scanner;

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
    }

    @PreUpdate
    void aktualisiereZeitstempel() {
        updatedAt = Instant.now();
    }
}
