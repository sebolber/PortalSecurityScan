package com.ahs.cvm.api.executive;

import com.ahs.cvm.ai.executive.ExecutiveReportService;
import com.ahs.cvm.ai.executive.ExecutiveReportService.Audience;
import com.ahs.cvm.ai.executive.ExecutiveReportService.ExecutiveReportResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "ExecutiveReport", description = "Executive-/Board-/Audit-Reports")
public class ExecutiveReportController {

    private final ExecutiveReportService service;

    public ExecutiveReportController(ExecutiveReportService service) {
        this.service = service;
    }

    @GetMapping(value = "/executive", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Liefert das Executive-PDF.")
    public ResponseEntity<byte[]> executive(
            @RequestParam("productVersionId") UUID productVersionId,
            @RequestParam("environmentId") UUID environmentId,
            @RequestParam(name = "audience", defaultValue = "board") String audienceParam,
            @RequestParam(name = "triggeredBy", defaultValue = "system:ui")
                    String triggeredBy) {
        Audience audience = "audit".equalsIgnoreCase(
                audienceParam == null ? "" : audienceParam.trim().toLowerCase(Locale.ROOT))
                ? Audience.AUDIT : Audience.BOARD;
        ExecutiveReportResult r = service.generate(
                productVersionId, environmentId, audience, triggeredBy);
        String fileName = "executive-" + audience.name().toLowerCase(Locale.ROOT) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .header("X-Executive-Ampel", r.summary().ampel())
                .body(r.pdf());
    }
}
