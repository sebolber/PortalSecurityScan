package com.ahs.cvm.persistence.scan;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComponentOccurrenceRepository
        extends JpaRepository<ComponentOccurrence, UUID> {
    List<ComponentOccurrence> findByScanId(UUID scanId);
}
