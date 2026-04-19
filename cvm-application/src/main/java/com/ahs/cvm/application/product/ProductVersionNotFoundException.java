package com.ahs.cvm.application.product;

import java.util.UUID;

public class ProductVersionNotFoundException extends RuntimeException {

    public ProductVersionNotFoundException(UUID productId, UUID versionId) {
        super("Produkt-Version nicht gefunden: product=" + productId + ", version=" + versionId);
    }
}
