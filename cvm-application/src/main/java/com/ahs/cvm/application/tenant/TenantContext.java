package com.ahs.cvm.application.tenant;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-lokaler Kontext fuer den aktuellen Mandanten (Iteration 21).
 *
 * <p>Gesetzt wird der Wert vom {@code JwtTenantResolver} im API-Modul,
 * gelesen vom Query-Layer (aktuell nur in ausgewaehlten Services als
 * Vorbereitung fuer die RLS-Durchsetzung in der Rollout-Phase).
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) {
        if (tenantId == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(tenantId);
        }
    }

    public static Optional<UUID> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static UUID requireCurrent() {
        UUID id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException("Kein Tenant-Kontext gesetzt.");
        }
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
