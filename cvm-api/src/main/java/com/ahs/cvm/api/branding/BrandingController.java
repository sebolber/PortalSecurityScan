package com.ahs.cvm.api.branding;

import com.ahs.cvm.application.branding.BrandingService;
import com.ahs.cvm.application.branding.BrandingUpdateCommand;
import com.ahs.cvm.application.branding.BrandingView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Branding-/Theming-Endpunkte (Iteration 27, CVM-61).
 *
 * <p>{@code GET /api/v1/theme} ist fuer alle angemeldeten User
 * lesbar und liefert die aktive Branding-Konfiguration.
 * {@code PUT /api/v1/admin/theme} erfordert {@code CVM_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Theming", description = "Mandantenspezifisches Branding")
public class BrandingController {

    private final BrandingService brandingService;

    public BrandingController(BrandingService brandingService) {
        this.brandingService = brandingService;
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
}
