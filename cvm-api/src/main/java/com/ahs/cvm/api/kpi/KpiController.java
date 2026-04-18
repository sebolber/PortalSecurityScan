package com.ahs.cvm.api.kpi;

import com.ahs.cvm.application.kpi.KpiService;
import com.ahs.cvm.application.kpi.KpiService.BurnDownPoint;
import com.ahs.cvm.application.kpi.KpiService.KpiResult;
import com.ahs.cvm.application.kpi.KpiService.SlaBucket;
import com.ahs.cvm.domain.enums.AhsSeverity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Fassade fuer KPIs + Trends (Iteration 21, CVM-52).
 *
 * <p>JSON fuer Dashboards, CSV fuer Executive-Anhaenge.
 */
@RestController
@RequestMapping("/api/v1/kpis")
@Tag(name = "KPI", description = "Trends, MTTR, Fix-SLA-Quote, Automatisierungsquote")
public class KpiController {

    private final KpiService service;

    public KpiController(KpiService service) {
        this.service = service;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "KPIs als JSON.")
    public ResponseEntity<KpiResult> json(
            @RequestParam(name = "productVersionId", required = false) UUID productVersionId,
            @RequestParam(name = "environmentId", required = false) UUID environmentId,
            @RequestParam(name = "window", defaultValue = "90d") String window) {
        return ResponseEntity.ok(service.compute(productVersionId, environmentId,
                parseWindow(window)));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "KPIs als CSV (Executive-Anhang).")
    public ResponseEntity<String> csv(
            @RequestParam(name = "productVersionId", required = false) UUID productVersionId,
            @RequestParam(name = "environmentId", required = false) UUID environmentId,
            @RequestParam(name = "window", defaultValue = "90d") String window) {
        KpiResult r = service.compute(productVersionId, environmentId,
                parseWindow(window));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition",
                        "attachment; filename=\"kpis.csv\"")
                .body(toCsv(r));
    }

    static Duration parseWindow(String raw) {
        if (raw == null || raw.isBlank()) {
            return Duration.ofDays(90);
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        try {
            if (s.endsWith("d")) {
                return Duration.ofDays(Integer.parseInt(s.substring(0, s.length() - 1)));
            }
            if (s.endsWith("h")) {
                return Duration.ofHours(Integer.parseInt(s.substring(0, s.length() - 1)));
            }
            return Duration.ofDays(Integer.parseInt(s));
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(
                    "Ungueltiges window-Format '" + raw + "' - erwartet z.B. '90d'");
        }
    }

    static String toCsv(KpiResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("section,severity,value\n");
        for (AhsSeverity s : AhsSeverity.values()) {
            sb.append("open,").append(s).append(',').append(r.openBySeverity().get(s)).append('\n');
        }
        for (AhsSeverity s : AhsSeverity.values()) {
            sb.append("mttr_days,").append(s).append(',').append(r.mttrDaysBySeverity().get(s)).append('\n');
        }
        for (AhsSeverity s : AhsSeverity.values()) {
            SlaBucket b = r.slaBySeverity().get(s);
            sb.append("sla_quote,").append(s).append(',');
            sb.append(String.format(Locale.ROOT, "%.4f", b.quote())).append('\n');
        }
        sb.append("automation,,").append(String.format(Locale.ROOT, "%.4f",
                r.automationRate())).append('\n');
        sb.append("\nday,open\n");
        for (BurnDownPoint p : r.burnDown()) {
            sb.append(p.day()).append(',').append(p.open()).append('\n');
        }
        return sb.toString();
    }
}
