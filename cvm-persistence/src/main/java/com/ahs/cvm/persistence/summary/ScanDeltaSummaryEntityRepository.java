package com.ahs.cvm.persistence.summary;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanDeltaSummaryEntityRepository
        extends JpaRepository<ScanDeltaSummaryEntity, UUID> {

    List<ScanDeltaSummaryEntity> findByScanIdOrderByCreatedAtDesc(UUID scanId);
}
