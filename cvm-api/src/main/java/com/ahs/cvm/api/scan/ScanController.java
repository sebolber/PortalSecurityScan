package com.ahs.cvm.api.scan;

import com.ahs.cvm.application.scan.ScanIngestService;
import com.ahs.cvm.application.scan.ScanSummary;
import com.ahs.cvm.application.scan.ScanUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/scans")
@Tag(name = "Scans", description = "SBOM-Ingestion und Scan-Status")
public class ScanController {

    private final ScanIngestService scanIngestService;

    public ScanController(ScanIngestService scanIngestService) {
        this.scanIngestService = scanIngestService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "CycloneDX-SBOM hochladen und Scan anstossen")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Upload akzeptiert, Verarbeitung laeuft asynchron"),
        @ApiResponse(responseCode = "400", description = "Invalide SBOM (Parse- oder Schema-Fehler) oder fehlende Umgebung"),
        @ApiResponse(responseCode = "409", description = "Scan mit identischem Inhalt wurde bereits verarbeitet")
    })
    public ResponseEntity<?> uploadMultipart(
            @RequestParam("productVersionId") UUID productVersionId,
            @RequestParam(value = "environmentId", required = false) UUID environmentId,
            @RequestParam(value = "scanner", defaultValue = "trivy") String scanner,
            @RequestParam("sbom") MultipartFile sbom)
            throws IOException {
        ResponseEntity<Map<String, Object>> envFehler = pruefeEnvironment(environmentId);
        if (envFehler != null) {
            return envFehler;
        }
        ScanUploadResponse response = scanIngestService.uploadAkzeptieren(
                productVersionId, environmentId, scanner, sbom.getBytes());
        return ResponseEntity.accepted()
                .header("Location", response.statusUri())
                .location(URI.create(response.statusUri()))
                .body(response);
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "CycloneDX-SBOM als Raw-JSON hochladen")
    public ResponseEntity<?> uploadRaw(
            @RequestParam("productVersionId") UUID productVersionId,
            @RequestParam(value = "environmentId", required = false) UUID environmentId,
            @RequestParam(value = "scanner", defaultValue = "trivy") String scanner,
            @org.springframework.web.bind.annotation.RequestBody byte[] rawJson) {
        ResponseEntity<Map<String, Object>> envFehler = pruefeEnvironment(environmentId);
        if (envFehler != null) {
            return envFehler;
        }
        ScanUploadResponse response = scanIngestService.uploadAkzeptieren(
                productVersionId, environmentId, scanner, rawJson);
        return ResponseEntity.accepted()
                .header("Location", response.statusUri())
                .location(URI.create(response.statusUri()))
                .body(response);
    }

    /**
     * Die Cascade ueberspringt Scans ohne environmentId still (Assessment.
     * environment ist NOT NULL). Frueher fuehrte das zu einer leeren Queue
     * trotz erfolgreichem Upload. Wir lehnen den Upload jetzt direkt mit
     * 400 ab, damit der Client einen klaren Fehler sieht.
     */
    private ResponseEntity<Map<String, Object>> pruefeEnvironment(UUID environmentId) {
        if (environmentId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "environment_required",
                            "message", "Umgebung ist Pflicht: ohne environmentId"
                                    + " waere die automatische Bewertung uebersprungen."));
        }
        return null;
    }

    @GetMapping("/{scanId}")
    @Operation(summary = "Status und Kennzahlen eines Scans abrufen")
    public ResponseEntity<ScanSummary> status(@PathVariable UUID scanId) {
        return scanIngestService
                .zusammenfassung(scanId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
