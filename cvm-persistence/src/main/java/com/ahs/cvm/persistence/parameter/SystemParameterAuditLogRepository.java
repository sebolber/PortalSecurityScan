package com.ahs.cvm.persistence.parameter;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemParameterAuditLogRepository
        extends JpaRepository<SystemParameterAuditLog, UUID> {

    List<SystemParameterAuditLog> findByTenantIdOrderByChangedAtDesc(
            UUID tenantId);

    List<SystemParameterAuditLog> findByParameterIdOrderByChangedAtDesc(
            UUID parameterId);
}
