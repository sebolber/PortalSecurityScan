package com.ahs.cvm.application.product;

import java.util.UUID;

public class ProductVersionConflictException extends RuntimeException {

    public ProductVersionConflictException(UUID productId, String version) {
        super("Version '" + version + "' fuer Produkt " + productId + " existiert bereits.");
    }
}
