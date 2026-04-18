package com.ahs.cvm.persistence.ai;

import com.ahs.cvm.domain.enums.AiCallStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiCallAuditRepository extends JpaRepository<AiCallAudit, UUID> {

    List<AiCallAudit> findByStatusAndCreatedAtBefore(AiCallStatus status, Instant grenze);

    long countByStatus(AiCallStatus status);
}
