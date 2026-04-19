package com.ahs.cvm.api.tenant;

import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.application.tenant.TenantLookupService;
import com.ahs.cvm.application.tenant.TenantView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Iteration 62 (CVM-62): Read-only Endpoint fuer "aktueller Mandant".
 * Die Shell-UI nutzt das, um den Namen des Mandanten in der Topbar
 * anzuzeigen, statt hartgecodeter Testdaten.
 *
 * <p>Der Endpoint liest den Mandanten aus dem {@link TenantContext}
 * (der vom {@code TenantContextFilter} aus dem JWT gesetzt wird) und
 * gibt die minimalen Metadaten zurueck. Keine Admin-Rolle noetig - jede
 * authentifizierte Person sieht ihren eigenen aktiven Mandanten.
 */
@RestController
@RequestMapping("/api/v1/tenant")
@Tag(name = "Tenant", description = "Aktueller Mandant (Read, alle Rollen)")
public class CurrentTenantController {

    private final TenantLookupService lookup;

    public CurrentTenantController(TenantLookupService lookup) {
        this.lookup = lookup;
    }

    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Aktuellen Mandanten zurueckgeben (aus JWT abgeleitet).")
    public ResponseEntity<TenantView> current() {
        UUID tenantId = TenantContext.current().orElse(null);
        if (tenantId == null) {
            return ResponseEntity.noContent().build();
        }
        return lookup.findById(tenantId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
