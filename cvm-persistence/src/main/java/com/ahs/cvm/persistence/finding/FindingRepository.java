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
}
