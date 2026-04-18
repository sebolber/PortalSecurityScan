package com.ahs.cvm.api.product;

import com.ahs.cvm.application.product.ProductKeyConflictException;
import com.ahs.cvm.application.product.ProductNotFoundException;
import com.ahs.cvm.application.product.ProductVersionConflictException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = ProductsController.class)
public class ProductsExceptionHandler {

    @ExceptionHandler(ProductKeyConflictException.class)
    public ResponseEntity<Map<String, Object>> keyKonflikt(
            ProductKeyConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "product_key_conflict",
                        "message", e.getMessage(),
                        "key", e.getKey()));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> nichtGefunden(
            ProductNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "product_not_found",
                        "message", e.getMessage()));
    }

    @ExceptionHandler(ProductVersionConflictException.class)
    public ResponseEntity<Map<String, Object>> versionKonflikt(
            ProductVersionConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "product_version_conflict",
                        "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> ungueltig(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "invalid_input",
                        "message", e.getMessage() == null ? "Ungueltige Eingabe." : e.getMessage()));
    }
}
