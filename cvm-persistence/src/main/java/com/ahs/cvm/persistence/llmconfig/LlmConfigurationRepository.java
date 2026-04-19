package com.ahs.cvm.persistence.llmconfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LlmConfigurationRepository
        extends JpaRepository<LlmConfiguration, UUID> {

    List<LlmConfiguration> findByTenantIdOrderByNameAsc(UUID tenantId);

    Optional<LlmConfiguration> findByTenantIdAndName(UUID tenantId, String name);

    Optional<LlmConfiguration> findByTenantIdAndActiveTrue(UUID tenantId);

    /**
     * Deaktiviert alle aktuell aktiven Konfigurationen eines Mandanten
     * ausser derjenigen mit {@code id}. Wird vom Service beim Setzen
     * von {@code active=true} aufgerufen, damit der partielle
     * Unique-Index {@code ux_llm_configuration_active_per_tenant}
     * nicht verletzt wird.
     */
    @Modifying
    @Query("UPDATE LlmConfiguration c "
            + "SET c.active = false "
            + "WHERE c.tenantId = :tenantId "
            + "  AND c.active = true "
            + "  AND (:excludeId IS NULL OR c.id <> :excludeId)")
    int deaktiviereAndereAktive(
            @Param("tenantId") UUID tenantId,
            @Param("excludeId") UUID excludeId);
}
