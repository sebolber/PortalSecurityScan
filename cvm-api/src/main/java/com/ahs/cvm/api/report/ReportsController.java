package com.ahs.cvm.api.report;

import com.ahs.cvm.application.report.GeneratedReportView;
import com.ahs.cvm.application.report.HardeningReportInput;
import com.ahs.cvm.application.report.ReportGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Endpunkte fuer PDF-Reports (Iteration 10, CVM-19).
 *
 * <ul>
 *   <li>{@code POST /api/v1/reports/hardening} &mdash; erzeugt einen
 *       Hardening-Report und liefert Metadaten (inkl. reportId und
 *       SHA-256).</li>
 *   <li>{@code GET /api/v1/reports/{reportId}} &mdash; liefert das
 *       archivierte PDF (application/pdf).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "PDF-Abschlussberichte")
public class ReportsController {

    private final ReportGeneratorService service;
    private final Clock clock;

    public ReportsController(ReportGeneratorService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PostMapping("/hardening")
    @Operation(summary = "Neuen Hardening-Report erzeugen (synchron, archiviert immutable)")
    public ResponseEntity<ReportResponse> erzeugen(
            @Valid @RequestBody HardeningReportRequest request) {
        Instant stichtag = request.stichtag() == null ? clock.instant() : request.stichtag();
        GeneratedReportView view = service.generateHardeningReport(
                new HardeningReportInput(
                        request.productVersionId(),
                        request.environmentId(),
                        request.gesamteinstufung(),
                        request.freigeberKommentar(),
                        request.erzeugtVon(),
                        stichtag));
        return ResponseEntity
                .created(URI.create("/api/v1/reports/" + view.reportId()))
                .body(ReportResponse.from(view));
    }

    @GetMapping(value = "/{reportId}", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Report-PDF herunterladen")
    public ResponseEntity<byte[]> download(@PathVariable UUID reportId) {
        GeneratedReportView view = service.findById(reportId);
        String fileName = "hardening-report-" + reportId + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .header("X-Report-Sha256", view.sha256())
                .body(view.pdfBytes());
    }

    @GetMapping("/{reportId}/meta")
    @Operation(summary = "Report-Metadaten (ohne PDF-Bytes)")
    public ResponseEntity<ReportResponse> meta(@PathVariable UUID reportId) {
        GeneratedReportView view = service.findById(reportId);
        return ResponseEntity.ok(ReportResponse.from(view));
    }

    @GetMapping
    @Operation(summary = "Report-Historie pagenieren (neueste zuerst, ohne PDF-Bytes)")
    public ResponseEntity<ReportListResponse> list(
            @RequestParam(name = "productVersionId", required = false) UUID productVersionId,
            @RequestParam(name = "environmentId", required = false) UUID environmentId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Page<GeneratedReportView> result = service.list(
                productVersionId, environmentId, page, size);
        List<ReportResponse> items = result.getContent().stream()
                .map(ReportResponse::from).toList();
        return ResponseEntity.ok(new ReportListResponse(
                items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }

    public record ReportListResponse(
            List<ReportResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {}
}
