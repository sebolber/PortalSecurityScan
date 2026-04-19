package com.ahs.cvm.persistence.environment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentRepository extends JpaRepository<Environment, UUID> {

    /**
     * Iteration 62B (CVM-62): Key ist nur innerhalb eines Mandanten
     * eindeutig.
     */
    Optional<Environment> findByTenantIdAndKey(UUID tenantId, String key);

    /**
     * Iteration 48 + 62B: nur aktive (nicht soft-geloeschte) Umgebungen
     * eines Mandanten.
     */
    List<Environment> findByTenantIdAndDeletedAtIsNullOrderByKeyAsc(UUID tenantId);

    /**
     * Cross-Tenant-Fallback - NUR fuer Admin-Use-Cases, die bewusst
     * ueber alle Mandanten lesen wollen.
     */
    List<Environment> findByDeletedAtIsNullOrderByKeyAsc();
}
