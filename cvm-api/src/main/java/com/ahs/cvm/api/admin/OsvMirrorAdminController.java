package com.ahs.cvm.api.admin;

import com.ahs.cvm.integration.osv.OsvJsonlMirrorLookup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-Endpunkt fuer den file-basierten OSV-Mirror aus
 * Iteration 72, Iteration 73 (CVM-310).
 *
 * <p>Aktiv nur, wenn der
 * {@link OsvJsonlMirrorLookup}-Bean im Context ist (er steht
 * unter {@code @ConditionalOnProperty}). Ist der Mirror nicht
 * aktiv, liefert der Endpunkt HTTP 503 mit klarer Fehler-
 * Kennzeichnung.
 */
@RestController
@RequestMapping("/api/v1/admin/osv-mirror")
@Tag(name = "Admin", description = "OSV-Mirror verwalten (air-gapped)")
public class OsvMirrorAdminController {

    private final Optional<OsvJsonlMirrorLookup> mirrorLookup;

    public OsvMirrorAdminController(Optional<OsvJsonlMirrorLookup> mirrorLookup) {
        this.mirrorLookup = mirrorLookup == null ? Optional.empty() : mirrorLookup;
    }

    @PostMapping("/reload")
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "OSV-Mirror-JSONL neu einlesen (ohne Neustart)")
    public ResponseEntity<Map<String, Object>> reload() {
        if (mirrorLookup.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "osv_mirror_inactive",
                            "message",
                            "OSV-Mirror ist nicht aktiviert "
                                    + "(cvm.enrichment.osv.mirror.enabled=false)."));
        }
        OsvJsonlMirrorLookup lookup = mirrorLookup.get();
        lookup.reload();
        Map<String, Object> body = Map.of(
                "reloaded", true,
                "indexSize", lookup.indexSize());
        return ResponseEntity.ok(body);
    }
}
