package com.ahs.cvm.persistence.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByKey(String key);

    /**
     * Liefert nur nicht-soft-gelöschte Produkte (Iteration 38,
     * CVM-82). Alphabetisch nach key sortiert - entspricht dem
     * bisherigen Admin-Listing.
     */
    List<Product> findByDeletedAtIsNullOrderByKeyAsc();
}
