package com.ahs.cvm.api.product;

import com.ahs.cvm.application.product.ProductQueryService;
import com.ahs.cvm.application.product.ProductVersionView;
import com.ahs.cvm.application.product.ProductView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-Endpunkt fuer Produkte und deren Versionen
 * (Iteration 26, CVM-57).
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Produkt-/Versions-Inventar (Read)")
public class ProductsController {

    private final ProductQueryService service;

    public ProductsController(ProductQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Alle Produkte auflisten (alphabetisch nach key).")
    public ResponseEntity<List<ProductView>> list() {
        return ResponseEntity.ok(service.listProducts());
    }

    @GetMapping("/{productId}/versions")
    @Operation(summary = "Versionen eines Produkts (neueste zuerst).")
    public ResponseEntity<List<ProductVersionView>> versions(
            @PathVariable UUID productId) {
        return ResponseEntity.ok(service.listVersions(productId));
    }
}
