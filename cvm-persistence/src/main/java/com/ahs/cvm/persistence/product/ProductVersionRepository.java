package com.ahs.cvm.persistence.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVersionRepository extends JpaRepository<ProductVersion, UUID> {

    Optional<ProductVersion> findByProductIdAndVersion(UUID productId, String version);

    List<ProductVersion> findByProductId(UUID productId);
}
