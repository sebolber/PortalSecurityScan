package com.ahs.cvm.api.branding;

import com.ahs.cvm.application.branding.BrandingAssetService;
import com.ahs.cvm.application.branding.BrandingAssetService.AssetKind;
import com.ahs.cvm.application.branding.BrandingAssetView;
import com.ahs.cvm.application.branding.BrandingService;
import com.ahs.cvm.application.branding.BrandingUpdateCommand;
import com.ahs.cvm.application.branding.BrandingView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Branding-/Theming-Endpunkte (Iterationen 27 und 27b, CVM-61/62).
 *
 * <p>{@code GET /api/v1/theme} ist fuer alle angemeldeten User
 * lesbar und liefert die aktive Branding-Konfiguration.
 * {@code PUT /api/v1/admin/theme} erfordert {@code CVM_ADMIN}.
 * {@code POST /api/v1/admin/theme/assets} akzeptiert Multipart-
 * Uploads fuer Logos, Favicons und Fonts (Iteration 27b).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Theming", description = "Mandantenspezifisches Branding")
public class BrandingController {

    private final BrandingService brandingService;
    private final BrandingAssetService assetService;

    public BrandingController(
            BrandingService brandingService, BrandingAssetService assetService) {
        this.brandingService = brandingService;
        this.assetService = assetService;
    }

    @GetMapping("/theme")
    @Operation(summary = "Aktive Branding-Konfiguration fuer den aktuellen Mandanten")
    public ResponseEntity<BrandingView> current() {
        return ResponseEntity.ok(brandingService.loadForCurrentTenant());
    }

    @PutMapping("/admin/theme")
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "Branding-Konfiguration aktualisieren (optimistisches Locking)")
    public ResponseEntity<BrandingView> update(
            @RequestBody BrandingUpdateCommand command, Principal principal) {
        String actor = principal != null ? principal.getName() : "anonymous";
        return ResponseEntity.ok(
                brandingService.updateForCurrentTenant(command, actor));
    }

    @PostMapping(value = "/admin/theme/assets", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "Branding-Asset (Logo/Favicon/Font) hochladen")
    public ResponseEntity<Map<String, Object>> uploadAsset(
            @RequestParam("kind") String kind,
            @RequestParam("file") MultipartFile file,
            Principal principal)
            throws IOException {
        AssetKind parsed = parseKind(kind);
        String actor = principal != null ? principal.getName() : "anonymous";
        BrandingAssetView saved = assetService.upload(
                parsed, file.getContentType(), file.getBytes(), actor);
        URI location = URI.create("/api/v1/theme/assets/" + saved.id());
        return ResponseEntity.created(location).body(Map.of(
                "id", saved.id(),
                "kind", saved.kind(),
                "contentType", saved.contentType(),
                "sizeBytes", saved.sizeBytes(),
                "sha256", saved.sha256(),
                "url", location.toString()));
    }

    @GetMapping("/theme/assets/{assetId}")
    @Operation(summary = "Branding-Asset abrufen (mit ETag aus SHA-256)")
    public ResponseEntity<byte[]> downloadAsset(@PathVariable UUID assetId) {
        return assetService
                .findById(assetId)
                .map(asset -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(asset.contentType()))
                        .eTag("\"" + asset.sha256() + "\"")
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(asset.bytes()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static AssetKind parseKind(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("kind-Parameter fehlt.");
        }
        try {
            return AssetKind.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unbekannter Asset-Kind: " + raw + " (erwartet LOGO, FAVICON oder FONT).");
        }
    }
}
