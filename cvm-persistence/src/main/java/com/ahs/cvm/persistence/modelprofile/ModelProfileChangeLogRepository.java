package com.ahs.cvm.persistence.modelprofile;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelProfileChangeLogRepository
        extends JpaRepository<ModelProfileChangeLog, UUID> {

    List<ModelProfileChangeLog> findByEnvironmentIdOrderByChangedAtDesc(
            UUID environmentId);
}
