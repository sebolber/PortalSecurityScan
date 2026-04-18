package com.ahs.cvm.persistence.report;

import com.ahs.cvm.domain.enums.AhsSeverity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Archivierter PDF-Abschlussbericht (Iteration 10, CVM-19).
 *
 * <p>Einmal gespeicherte Reports sind unveraenderlich. Der
 * {@link GeneratedReportImmutabilityListener} verhindert UPDATEs
 * analog zum {@code Assessment}.
 */
@Entity
@Table(name = "generated_report")
@EntityListeners(GeneratedReportImmutabilityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GeneratedReport {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product_version_id", nullable = false, updatable = false)
    private UUID productVersionId;

    @Column(name = "environment_id", nullable = false, updatable = false)
    private UUID environmentId;

    @Column(name = "report_type", nullable = false, updatable = false)
    private String reportType;

    @Column(name = "title", nullable = false, updatable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "gesamteinstufung", nullable = false, updatable = false)
    private AhsSeverity gesamteinstufung;

    @Column(name = "freigeber_kommentar", updatable = false)
    private String freigeberKommentar;

    @Column(name = "erzeugt_von", nullable = false, updatable = false)
    private String erzeugtVon;

    @Column(name = "erzeugt_am", nullable = false, updatable = false)
    private Instant erzeugtAm;

    @Column(name = "stichtag", nullable = false, updatable = false)
    private Instant stichtag;

    @Column(name = "sha256", nullable = false, updatable = false, length = 64)
    private String sha256;

    /**
     * PDF-Rohbytes. {@code @Lob} wuerde Hibernate 6 zu einem Postgres-
     * OID/BLOB greifen lassen und mit der {@code BYTEA}-Migration
     * kollidieren. Daher explizit als VARBINARY gemappt.
     */
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "pdf_bytes", nullable = false, updatable = false)
    private byte[] pdfBytes;

    @PrePersist
    void initialisiere() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (erzeugtAm == null) {
            erzeugtAm = Instant.now();
        }
        if (reportType == null) {
            reportType = "HARDENING";
        }
    }
}
