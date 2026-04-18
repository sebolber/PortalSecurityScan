package com.ahs.cvm.persistence.report;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, UUID> {

    /**
     * Liefert alle Reports fuer eine Produkt-/Umgebungs-Kombination,
     * sortiert nach Erzeugungszeitpunkt absteigend.
     */
    List<GeneratedReport>
            findByProductVersionIdAndEnvironmentIdOrderByErzeugtAmDesc(
                    UUID productVersionId, UUID environmentId);
}
