package com.ahs.cvm.persistence.ai;

import com.ahs.cvm.domain.enums.AiCallStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiCallAuditRepository extends JpaRepository<AiCallAudit, UUID> {

    List<AiCallAudit> findByStatusAndCreatedAtBefore(AiCallStatus status, Instant grenze);

    long countByStatus(AiCallStatus status);

    /**
     * Listing fuer den AI-Audit-Endpoint (Lese-Sicht). Filter auf
     * Status und Use-Case sind optional ({@code null} = keine
     * Einschraenkung). Sortiert ueber {@link Pageable}.
     */
    @Query("SELECT a FROM AiCallAudit a "
            + "WHERE (:status IS NULL OR a.status = :status) "
            + "  AND (:useCase IS NULL OR a.useCase = :useCase)")
    Page<AiCallAudit> findeAudits(
            @Param("status") AiCallStatus status,
            @Param("useCase") String useCase,
            Pageable pageable);
}
