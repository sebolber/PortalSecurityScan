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
import java.util.UUID;
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
        @ApiResponse(responseCode = "400", description = "Invalide SBOM (Parse- oder Schema-Fehler)"),
        @ApiResponse(responseCode = "409", description = "Scan mit identischem Inhalt wurde bereits verarbeitet")
    })
    public ResponseEntity<ScanUploadResponse> uploadMultipart(
            @RequestParam("productVersionId") UUID productVersionId,
            @RequestParam(value = "environmentId", required = false) UUID environmentId,
            @RequestParam(value = "scanner", defaultValue = "trivy") String scanner,
            @RequestParam("sbom") MultipartFile sbom)
            throws IOException {
        ScanUploadResponse response = scanIngestService.uploadAkzeptieren(
                productVersionId, environmentId, scanner, sbom.getBytes());
        return ResponseEntity.accepted()
                .header("Location", response.statusUri())
                .location(URI.create(response.statusUri()))
                .body(response);
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "CycloneDX-SBOM als Raw-JSON hochladen")
    public ResponseEntity<ScanUploadResponse> uploadRaw(
            @RequestParam("productVersionId") UUID productVersionId,
            @RequestParam(value = "environmentId", required = false) UUID environmentId,
            @RequestParam(value = "scanner", defaultValue = "trivy") String scanner,
            @org.springframework.web.bind.annotation.RequestBody byte[] rawJson) {
        ScanUploadResponse response = scanIngestService.uploadAkzeptieren(
                productVersionId, environmentId, scanner, rawJson);
        return ResponseEntity.accepted()
                .header("Location", response.statusUri())
                .location(URI.create(response.statusUri()))
                .body(response);
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
