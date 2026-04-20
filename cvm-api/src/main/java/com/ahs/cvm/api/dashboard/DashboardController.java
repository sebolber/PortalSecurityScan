package com.ahs.cvm.api.dashboard;

import com.ahs.cvm.application.dashboard.DashboardKpiService;
import com.ahs.cvm.application.dashboard.DashboardKpiView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Iteration 100 (CVM-342): Dashboard-KPI-Endpoint. Liefert die
 * Zahlen, die frueher im Frontend hart-codiert waren.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "KPIs fuer die Startseite")
public class DashboardController {

    private final DashboardKpiService service;

    public DashboardController(DashboardKpiService service) {
        this.service = service;
    }

    @GetMapping("/kpi")
    @Operation(summary = "Offene CVEs, Severity-Verteilung, aeltestes CRITICAL.")
    public ResponseEntity<DashboardKpiView> kpi() {
        return ResponseEntity.ok(service.berechne());
    }
}
