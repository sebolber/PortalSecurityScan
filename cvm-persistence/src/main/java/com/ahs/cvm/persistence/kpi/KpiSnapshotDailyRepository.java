package com.ahs.cvm.persistence.kpi;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KpiSnapshotDailyRepository extends JpaRepository<KpiSnapshotDaily, UUID> {

    @Query("""
            SELECT s FROM KpiSnapshotDaily s
            WHERE s.snapshotDay = :day
              AND (:pv IS NULL AND s.productVersionId IS NULL
                   OR s.productVersionId = :pv)
              AND (:env IS NULL AND s.environmentId IS NULL
                   OR s.environmentId = :env)
            """)
    Optional<KpiSnapshotDaily> findByScope(
            @Param("day") LocalDate day,
            @Param("pv") UUID productVersionId,
            @Param("env") UUID environmentId);

    List<KpiSnapshotDaily> findByProductVersionIdAndEnvironmentIdOrderBySnapshotDayDesc(
            UUID productVersionId, UUID environmentId);
}
