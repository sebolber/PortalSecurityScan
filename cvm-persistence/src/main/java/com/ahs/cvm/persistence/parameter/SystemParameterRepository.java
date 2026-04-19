package com.ahs.cvm.persistence.parameter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemParameterRepository
        extends JpaRepository<SystemParameter, UUID> {

    List<SystemParameter> findByTenantIdOrderByCategoryAscLabelAsc(UUID tenantId);

    List<SystemParameter> findByTenantIdAndCategoryOrderByLabelAsc(
            UUID tenantId, String category);

    Optional<SystemParameter> findByTenantIdAndParamKey(
            UUID tenantId, String paramKey);
}
