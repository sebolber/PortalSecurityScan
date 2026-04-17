package com.ahs.cvm.persistence.environment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentRepository extends JpaRepository<Environment, UUID> {
    Optional<Environment> findByKey(String key);
}
