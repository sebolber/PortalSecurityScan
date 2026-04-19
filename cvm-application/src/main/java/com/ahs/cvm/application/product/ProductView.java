package com.ahs.cvm.application.product;

import com.ahs.cvm.persistence.product.Product;
import java.util.UUID;

/**
 * DTO fuer Produktlisten/-detail. Seit Iteration 76 (CVM-313)
 * optional mit {@code repoUrl} (Git-Repository fuer Reachability).
 */
public record ProductView(
        UUID id, String key, String name, String description, String repoUrl) {

    public static ProductView from(Product p) {
        return new ProductView(
                p.getId(), p.getKey(), p.getName(), p.getDescription(),
                p.getRepoUrl());
    }
}
