package com.ahs.cvm.application.product;

import com.ahs.cvm.persistence.product.Product;
import java.util.UUID;

public record ProductView(
        UUID id, String key, String name, String description) {

    public static ProductView from(Product p) {
        return new ProductView(
                p.getId(), p.getKey(), p.getName(), p.getDescription());
    }
}
