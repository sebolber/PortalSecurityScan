package com.ahs.cvm.api.summary;

import com.ahs.cvm.ai.summary.ScanDelta;
import com.ahs.cvm.ai.summary.ScanDelta.SeverityShift;
import com.ahs.cvm.ai.summary.ScanDeltaSummary;
import com.ahs.cvm.ai.summary.ScanDeltaSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Endpoint zur KI-Delta-Summary (Iteration 14, CVM-33).
 *
 * <p>Audience steuert, ob {@code shortText} (Slack) oder
 * {@code longText} (Lenkungsausschuss) als {@code summary}-Feld
 * geliefert wird; zusaetzlich enthaelt die Antwort den
 * strukturierten Diff.
 */
@RestController
@RequestMapping("/api/v1/scans")
@Tag(name = "DeltaSummary", description = "KI-Delta-Summary pro Scan")
public class DeltaSummaryController {

    private final ScanDeltaSummaryService service;

    public DeltaSummaryController(ScanDeltaSummaryService service) {
        this.service = service;
    }

    @GetMapping("/{id}/delta-summary")
    @Operation(summary = "Liefert die KI-Delta-Summary fuer einen Scan.")
    public ResponseEntity<DeltaSummaryResponse> get(
            @PathVariable("id") UUID scanId,
            @RequestParam(name = "audience", defaultValue = "short") String audience) {
        ScanDeltaSummary s = service.summarize(scanId);
        boolean longAudience = "long".equalsIgnoreCase(audience.trim().toLowerCase(Locale.ROOT));
        String summary = longAudience ? s.longText() : s.shortText();
        return ResponseEntity.ok(new DeltaSummaryResponse(
                s.scanId(),
                s.previousScanId(),
                summary,
                s.llmAufgerufen(),
                s.delta().neueCves(),
                s.delta().entfalleneCves(),
                s.delta().severityShifts(),
                s.delta().kevAenderungen()));
    }

    public record DeltaSummaryResponse(
            UUID scanId,
            UUID previousScanId,
            String summary,
            boolean llmAufgerufen,
            List<String> neueCves,
            List<String> entfalleneCves,
            List<SeverityShift> severityShifts,
            List<String> kevAenderungen) {}
}
