package com.ahs.cvm.persistence.branding;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandingConfigHistoryRepository
        extends JpaRepository<BrandingConfigHistory, UUID> {

    List<BrandingConfigHistory> findByTenantIdOrderByVersionDesc(
            UUID tenantId, Pageable pageable);

    Optional<BrandingConfigHistory> findByTenantIdAndVersion(UUID tenantId, int version);
}
