package com.ahs.cvm.persistence.finding;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FindingRepository extends JpaRepository<Finding, UUID> {

    List<Finding> findByScanId(UUID scanId);

    List<Finding> findByCveId(UUID cveId);

    @Query("select distinct f.cve.cveId from Finding f where f.scan.id = :scanId")
    List<String> findCveIdsByScanId(UUID scanId);

    List<Finding> findByDetectedAtBetween(Instant start, Instant end);

    /**
     * Dedup-Helfer fuer Iteration 33: ein OSV-Matching-Run darf bei
     * wiederholtem Aufruf keine doppelten Findings anlegen. Primary
     * Key ist (scan, componentOccurrence, cve). Die Query nutzt den
     * bestehenden BTree-Index auf diesen drei Spalten.
     */
    boolean existsByScanIdAndComponentOccurrenceIdAndCveCveId(
            UUID scanId, UUID componentOccurrenceId, String cveId);
}
