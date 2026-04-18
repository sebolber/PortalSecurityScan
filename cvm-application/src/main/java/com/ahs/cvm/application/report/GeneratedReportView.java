package com.ahs.cvm.application.report;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.persistence.report.GeneratedReport;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-Model fuer den REST-Rueckgabewert nach einer Report-Erzeugung.
 * Haelt die Arch-Regel {@code api -> persistence} aus dem Controller
 * fern.
 */
public record GeneratedReportView(
        UUID reportId,
        UUID productVersionId,
        UUID environmentId,
        String reportType,
        String title,
        AhsSeverity gesamteinstufung,
        String erzeugtVon,
        Instant erzeugtAm,
        Instant stichtag,
        String sha256,
        byte[] pdfBytes) {

    public static GeneratedReportView from(GeneratedReport r) {
        return new GeneratedReportView(
                r.getId(),
                r.getProductVersionId(),
                r.getEnvironmentId(),
                r.getReportType(),
                r.getTitle(),
                r.getGesamteinstufung(),
                r.getErzeugtVon(),
                r.getErzeugtAm(),
                r.getStichtag(),
                r.getSha256(),
                r.getPdfBytes());
    }

    public GeneratedReportView withoutBytes() {
        return new GeneratedReportView(
                reportId, productVersionId, environmentId, reportType,
                title, gesamteinstufung, erzeugtVon, erzeugtAm,
                stichtag, sha256, null);
    }
}
