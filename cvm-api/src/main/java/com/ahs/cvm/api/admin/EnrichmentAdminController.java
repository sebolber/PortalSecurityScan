package com.ahs.cvm.api.admin;

import com.ahs.cvm.application.cve.CveEnrichmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-Endpunkte fuer manuelles Re-Fetch der CVE-Feeds. Nur fuer
 * Rolle {@code CVM_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/admin/enrichment")
@Tag(name = "Admin", description = "Manuelle Anreicherung und Betrieb")
public class EnrichmentAdminController {

    private final CveEnrichmentService enrichmentService;

    public EnrichmentAdminController(CveEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "Alle oder einen Feed manuell neu laden")
    public ResponseEntity<Map<String, Object>> refresh(
            @RequestParam(value = "source", defaultValue = "all") String source) {
        int anzahl = enrichmentService.refreshAll(Optional.of(source));
        return ResponseEntity.ok(Map.of(
                "source", source,
                "processed", anzahl));
    }
}
