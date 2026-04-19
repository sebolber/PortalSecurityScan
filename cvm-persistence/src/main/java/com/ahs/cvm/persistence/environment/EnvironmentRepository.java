package com.ahs.cvm.persistence.environment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentRepository extends JpaRepository<Environment, UUID> {

    Optional<Environment> findByKey(String key);

    /**
     * Iteration 48 (CVM-98): nur aktive (nicht soft-geloeschte) Umgebungen.
     */
    List<Environment> findByDeletedAtIsNullOrderByKeyAsc();
}
