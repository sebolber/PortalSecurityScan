package com.ahs.cvm.persistence.environment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentDeploymentRepository
        extends JpaRepository<EnvironmentDeployment, UUID> {

    List<EnvironmentDeployment> findByEnvironmentId(UUID environmentId);

    List<EnvironmentDeployment> findByProductVersionId(UUID productVersionId);
}
