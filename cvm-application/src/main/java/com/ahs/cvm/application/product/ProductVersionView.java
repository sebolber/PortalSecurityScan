package com.ahs.cvm.application.product;

import com.ahs.cvm.persistence.product.ProductVersion;
import java.time.Instant;
import java.util.UUID;

public record ProductVersionView(
        UUID id,
        UUID productId,
        String version,
        String gitCommit,
        Instant releasedAt) {

    public static ProductVersionView from(ProductVersion v) {
        UUID productId = v.getProduct() == null ? null : v.getProduct().getId();
        return new ProductVersionView(
                v.getId(),
                productId,
                v.getVersion(),
                v.getGitCommit(),
                v.getReleasedAt());
    }
}
