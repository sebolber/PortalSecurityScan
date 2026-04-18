package com.ahs.cvm.api.report;

import com.ahs.cvm.application.report.GeneratedReportView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-Model fuer REST-Antworten zu Reports.
 * Enthaelt nur die Metadaten (nicht die PDF-Bytes).
 */
public record ReportResponse(
        UUID reportId,
        UUID productVersionId,
        UUID environmentId,
        String reportType,
        String title,
        AhsSeverity gesamteinstufung,
        String erzeugtVon,
        Instant erzeugtAm,
        Instant stichtag,
        String sha256) {

    public static ReportResponse from(GeneratedReportView v) {
        return new ReportResponse(
                v.reportId(),
                v.productVersionId(),
                v.environmentId(),
                v.reportType(),
                v.title(),
                v.gesamteinstufung(),
                v.erzeugtVon(),
                v.erzeugtAm(),
                v.stichtag(),
                v.sha256());
    }
}
