package com.ahs.cvm.persistence.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Iteration 62A (CVM-62): Key ist nur innerhalb eines Mandanten
     * eindeutig. Lookups muessen die Tenant-ID mitgeben.
     */
    Optional<Product> findByTenantIdAndKey(UUID tenantId, String key);

    /**
     * Liefert nur nicht-soft-geloeschte Produkte eines Mandanten
     * (Iteration 38, CVM-82 + Iteration 62A). Alphabetisch nach key.
     */
    List<Product> findByTenantIdAndDeletedAtIsNullOrderByKeyAsc(UUID tenantId);

    /**
     * Fallback ohne Tenant-Filter - NUR fuer Admin-Use-Cases, die
     * explizit alle Mandanten sehen duerfen (z.B. Cross-Tenant-Reports).
     * Alle sonstigen Callsites muessen den Tenant uebergeben.
     */
    List<Product> findByDeletedAtIsNullOrderByKeyAsc();
}
