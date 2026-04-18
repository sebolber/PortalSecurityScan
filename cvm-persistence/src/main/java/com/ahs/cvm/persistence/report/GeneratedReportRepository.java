package com.ahs.cvm.persistence.report;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, UUID> {

    /**
     * Liefert alle Reports fuer eine Produkt-/Umgebungs-Kombination,
     * sortiert nach Erzeugungszeitpunkt absteigend.
     */
    List<GeneratedReport>
            findByProductVersionIdAndEnvironmentIdOrderByErzeugtAmDesc(
                    UUID productVersionId, UUID environmentId);

    /**
     * Pagenierte Liste fuer den Reports-Listing-Endpoint
     * (Frontend-Nachzug 10/11). Filter auf {@code productVersionId}
     * und {@code environmentId} sind optional ({@code null} = keine
     * Einschraenkung).
     */
    Page<GeneratedReport> findByProductVersionIdAndEnvironmentId(
            UUID productVersionId, UUID environmentId, Pageable pageable);

    Page<GeneratedReport> findByProductVersionId(UUID productVersionId, Pageable pageable);

    Page<GeneratedReport> findByEnvironmentId(UUID environmentId, Pageable pageable);
}
