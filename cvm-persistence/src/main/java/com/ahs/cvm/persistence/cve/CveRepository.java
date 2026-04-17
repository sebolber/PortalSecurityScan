package com.ahs.cvm.persistence.cve;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CveRepository extends JpaRepository<Cve, UUID> {
    Optional<Cve> findByCveId(String cveId);
}
