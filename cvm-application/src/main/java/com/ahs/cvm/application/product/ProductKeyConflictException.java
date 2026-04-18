package com.ahs.cvm.application.product;

public class ProductKeyConflictException extends RuntimeException {

    private final String key;

    public ProductKeyConflictException(String key) {
        super("Produkt-Key '" + key + "' bereits vergeben.");
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
