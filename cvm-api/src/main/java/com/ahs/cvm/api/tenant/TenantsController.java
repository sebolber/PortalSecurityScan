package com.ahs.cvm.api.tenant;

import com.ahs.cvm.application.tenant.TenantLookupService;
import com.ahs.cvm.application.tenant.TenantView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Iteration 56 (CVM-106): Read-only Admin-Liste der Mandanten.
 * Anlage/Aktivierung/Deaktivierung bleibt Admin-SQL; eine volle CRUD-
 * UI erfordert Multi-Tenant-Flows (separate Iteration).
 */
@RestController
@RequestMapping("/api/v1/admin/tenants")
@Tag(name = "Tenants", description = "Mandanten-Uebersicht (Admin)")
public class TenantsController {

    private final TenantLookupService lookup;

    public TenantsController(TenantLookupService lookup) {
        this.lookup = lookup;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CVM_ADMIN')")
    @Operation(summary = "Alle Mandanten auflisten (Admin).")
    public ResponseEntity<List<TenantView>> list() {
        return ResponseEntity.ok(lookup.listAll());
    }
}
