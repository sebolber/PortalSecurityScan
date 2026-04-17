package com.ahs.cvm.persistence.scan;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComponentRepository extends JpaRepository<Component, UUID> {
    Optional<Component> findByPurl(String purl);
}
