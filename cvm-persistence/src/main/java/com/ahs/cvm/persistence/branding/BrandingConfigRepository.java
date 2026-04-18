package com.ahs.cvm.persistence.branding;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandingConfigRepository extends JpaRepository<BrandingConfig, UUID> {

    Optional<BrandingConfig> findByTenantId(UUID tenantId);
}
