package com.ahs.cvm.persistence.profile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContextProfileRepository extends JpaRepository<ContextProfile, UUID> {

    List<ContextProfile> findByEnvironmentIdOrderByVersionNumberDesc(UUID environmentId);

    Optional<ContextProfile> findFirstByEnvironmentIdOrderByVersionNumberDesc(UUID environmentId);
}
