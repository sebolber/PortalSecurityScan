package com.ahs.cvm.application.tenant;

import com.ahs.cvm.persistence.tenant.Tenant;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-Modell fuer die Admin-Mandanten-Liste (Iteration 56, CVM-106).
 */
public record TenantView(
        UUID id,
        String tenantKey,
        String name,
        boolean active,
        boolean defaultTenant,
        Instant createdAt) {

    public static TenantView from(Tenant t) {
        return new TenantView(
                t.getId(),
                t.getTenantKey(),
                t.getName(),
                t.isActive(),
                t.isDefaultTenant(),
                t.getCreatedAt());
    }
}
