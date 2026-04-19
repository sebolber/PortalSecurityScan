package com.ahs.cvm.persistence.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVersionRepository extends JpaRepository<ProductVersion, UUID> {

    Optional<ProductVersion> findByProductIdAndVersion(UUID productId, String version);

    List<ProductVersion> findByProductId(UUID productId);

    /**
     * Iteration 49 (CVM-99): nur aktive (nicht soft-geloeschte) Versionen.
     */
    List<ProductVersion> findByProductIdAndDeletedAtIsNull(UUID productId);

    /**
     * Iteration 49 (CVM-99): aktive Version eines Produkts zu einer
     * Versionsnummer (fuer Dubletten-Check beim Anlegen).
     */
    Optional<ProductVersion> findByProductIdAndVersionAndDeletedAtIsNull(
            UUID productId, String version);
}
