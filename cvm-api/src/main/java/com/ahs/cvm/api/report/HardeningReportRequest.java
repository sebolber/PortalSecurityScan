package com.ahs.cvm.api.report;

import com.ahs.cvm.domain.enums.AhsSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Request-Body fuer {@code POST /api/v1/reports/hardening}.
 *
 * <p>{@code stichtag} ist optional; default ist der Server-Zeitpunkt
 * aus dem injizierten {@code Clock}.
 */
public record HardeningReportRequest(
        @NotNull UUID productVersionId,
        @NotNull UUID environmentId,
        @NotNull AhsSeverity gesamteinstufung,
        String freigeberKommentar,
        @NotBlank String erzeugtVon,
        Instant stichtag) {}
