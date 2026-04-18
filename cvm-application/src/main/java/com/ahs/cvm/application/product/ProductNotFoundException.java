package com.ahs.cvm.application.product;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID productId) {
        super("Produkt nicht gefunden: " + productId);
    }
}
