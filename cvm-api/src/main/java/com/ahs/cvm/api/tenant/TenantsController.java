package com.ahs.cvm.api.tenant;

import com.ahs.cvm.application.tenant.TenantLookupService;
import com.ahs.cvm.application.tenant.TenantLookupService.TenantKeyAlreadyExistsException;
import com.ahs.cvm.application.tenant.TenantView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Iteration 56 (CVM-106): Read-only Admin-Liste der Mandanten.
 * Iteration 59 (CVM-109): Anlage-POST. Aktivierung im Keycloak-
 * Mapping bleibt separat.
 */
@RestController
@RequestMapping("/api/v1/admin/tenants")
@Tag(name = "Tenants", description = "Mandanten-Uebersicht und Anlage (Admin)")
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

    @PostMapping
    @PreAuthorize("hasAuthority('CVM_ADMIN')")
    @Operation(summary = "Neuen Mandanten anlegen (Admin).")
    public ResponseEntity<TenantView> create(
            @Valid @RequestBody TenantCreateRequest request) {
        TenantView saved = lookup.create(
                request.tenantKey(),
                request.name(),
                request.active() == null ? Boolean.TRUE : request.active());
        return ResponseEntity
                .created(URI.create("/api/v1/admin/tenants/" + saved.id()))
                .body(saved);
    }

    @ExceptionHandler(TenantKeyAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
            TenantKeyAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "tenant_key_exists", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "tenant_validation", "message", ex.getMessage()));
    }

    public record TenantCreateRequest(
            @NotBlank String tenantKey,
            @NotBlank String name,
            Boolean active) {}
}
