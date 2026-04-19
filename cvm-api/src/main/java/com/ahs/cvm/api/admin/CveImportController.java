package com.ahs.cvm.api.admin;

import com.ahs.cvm.application.cve.CveFeedImportService;
import com.ahs.cvm.application.cve.CveFeedImportService.ImportReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin-Endpunkt: hochgeladene CVE-Datei (JSON) importieren. Zielgruppe
 * sind air-gapped Installationen oder Dev-/Test-Setups ohne Online-Zugriff
 * auf NVD (Iteration 61, CVM-62).
 */
@RestController
@RequestMapping("/api/v1/admin/cves")
@Tag(name = "Admin", description = "CVE-Daten manuell importieren")
public class CveImportController {

    private final CveFeedImportService importService;

    public CveImportController(CveFeedImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "CVE-JSON-Datei importieren",
            description = "Akzeptiert NVD-2.0-Format (Objekt mit "
                    + "vulnerabilities[]) oder eine flache Liste von CVE-Objekten. "
                    + "Idempotent: bestehende CVEs werden geupdated, neue angelegt.")
    public ResponseEntity<ImportResponse> importCves(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "source", required = false, defaultValue = "UPLOAD") String source)
            throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ImportResponse(0, 0, 0, 0, "Datei fehlt oder ist leer."));
        }
        ImportReport report;
        try {
            report = importService.importFrom(file.getInputStream(), source);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                    new ImportResponse(0, 0, 0, 0, ex.getMessage()));
        }
        return ResponseEntity.ok(new ImportResponse(
                report.gefundeneEintraege(),
                report.angelegt(),
                report.aktualisiert(),
                report.fehlerCount(),
                null));
    }

    public record ImportResponse(
            int gefundeneEintraege,
            int angelegt,
            int aktualisiert,
            int fehlerCount,
            String fehlerHinweis) {}
}
