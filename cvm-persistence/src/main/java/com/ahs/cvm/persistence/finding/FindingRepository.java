package com.ahs.cvm.persistence.finding;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingRepository extends JpaRepository<Finding, UUID> {

    List<Finding> findByScanId(UUID scanId);

    List<Finding> findByCveId(UUID cveId);
}
