package com.ahs.cvm.application.report;

import java.util.UUID;

/** Wird geworfen, wenn ein gesuchter {@code GeneratedReport} nicht existiert. */
public class ReportNotFoundException extends RuntimeException {

    private final UUID reportId;

    public ReportNotFoundException(UUID reportId) {
        super("Report nicht gefunden: " + reportId);
        this.reportId = reportId;
    }

    public UUID reportId() {
        return reportId;
    }
}
