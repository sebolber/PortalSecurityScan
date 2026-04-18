package com.ahs.cvm.api.vex;

import com.ahs.cvm.application.vex.VexExporter;
import com.ahs.cvm.application.vex.VexImporter;
import com.ahs.cvm.application.vex.VexImporter.Parsed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * VEX-Endpoints (Iteration 20, CVM-51).
 *
 * <p>{@code GET /{productVersionId}}: liefert VEX in CycloneDX oder
 * CSAF 2.0. {@code POST /import}: nimmt ein VEX-Dokument entgegen,
 * validiert es und meldet Statements + Warnungen + Fehler. Der Import
 * schreibt (wie im Plan vorgesehen) <b>nichts</b> direkt in die DB -
 * die Statements laufen durch die normale Bewertungs-Queue.
 */
@RestController
@RequestMapping("/api/v1/vex")
@Tag(name = "VEX", description = "VEX-Export (CycloneDX/CSAF) und -Import")
public class VexController {

    private final VexExporter exporter;
    private final VexImporter importer;

    public VexController(VexExporter exporter, VexImporter importer) {
        this.exporter = exporter;
        this.importer = importer;
    }

    @GetMapping(value = "/{productVersionId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "VEX-Export fuer eine ProductVersion.")
    public ResponseEntity<String> export(
            @PathVariable("productVersionId") UUID productVersionId,
            @RequestParam(name = "format", defaultValue = "cyclonedx") String format) {
        String f = format == null ? "cyclonedx" : format.trim().toLowerCase(Locale.ROOT);
        if (!VexExporter.FORMAT_CYCLONEDX.equals(f)
                && !VexExporter.FORMAT_CSAF.equals(f)) {
            throw new IllegalArgumentException(
                    "Unbekanntes VEX-Format '" + format
                            + "' - erlaubt: cyclonedx, csaf");
        }
        String body = exporter.export(productVersionId, f);
        String fileName = "vex-" + productVersionId + "-" + f + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .header("X-VEX-Format", f)
                .body(body);
    }

    @PostMapping(value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "VEX-Dokument einlesen (nur parse + validieren).")
    public ResponseEntity<Map<String, Object>> importDoc(
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Datei fehlt oder ist leer.");
        }
        String doc = new String(file.getBytes(), StandardCharsets.UTF_8);
        Parsed parsed = importer.parse(doc);
        Map<String, Object> body = Map.of(
                "statements", parsed.statements(),
                "warnings", parsed.warnings(),
                "errors", parsed.errors());
        if (!parsed.errors().isEmpty()) {
            return ResponseEntity.status(422).body(body);
        }
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/import",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "VEX-Dokument als JSON-Body einlesen.")
    public ResponseEntity<Map<String, Object>> importJson(
            @org.springframework.web.bind.annotation.RequestBody String doc) {
        Parsed parsed = importer.parse(doc == null ? "" : doc);
        Map<String, Object> body = Map.of(
                "statements", parsed.statements(),
                "warnings", parsed.warnings(),
                "errors", parsed.errors());
        if (!parsed.errors().isEmpty()) {
            return ResponseEntity.status(422).body(body);
        }
        return ResponseEntity.ok(body);
    }

    /** Hilfs-Record fuer OpenAPI-Klarheit. */
    public record ImportReport(
            List<Object> statements, List<String> warnings, List<String> errors) {}
}
