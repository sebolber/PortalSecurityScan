package com.ahs.cvm.persistence.scan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanRepository extends JpaRepository<Scan, UUID> {

    Optional<Scan> findByProductVersionIdAndSbomChecksum(
            UUID productVersionId, String sbomChecksum);

    List<Scan> findByProductVersionIdOrderByScannedAtDesc(UUID productVersionId);
}
