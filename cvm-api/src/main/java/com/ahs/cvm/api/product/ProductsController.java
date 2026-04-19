package com.ahs.cvm.api.product;

import com.ahs.cvm.application.product.ProductCatalogService;
import com.ahs.cvm.application.product.ProductCatalogService.ProductCreateInput;
import com.ahs.cvm.application.product.ProductCatalogService.ProductUpdateInput;
import com.ahs.cvm.application.product.ProductCatalogService.ProductVersionCreateInput;
import com.ahs.cvm.application.product.ProductQueryService;
import com.ahs.cvm.application.product.ProductVersionView;
import com.ahs.cvm.application.product.ProductView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Produkt- und Versions-Verwaltung. Die Read-Endpunkte sind fuer alle
 * authentifizierten Nutzer sichtbar, Schreiboperationen verlangen
 * {@code CVM_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Produkt-/Versions-Inventar (Read + Admin-Create)")
public class ProductsController {

    private final ProductQueryService queryService;
    private final ProductCatalogService catalogService;

    public ProductsController(
            ProductQueryService queryService,
            ProductCatalogService catalogService) {
        this.queryService = queryService;
        this.catalogService = catalogService;
    }

    @GetMapping
    @Operation(summary = "Alle Produkte auflisten (alphabetisch nach key).")
    public ResponseEntity<List<ProductView>> list() {
        return ResponseEntity.ok(queryService.listProducts());
    }

    @GetMapping("/{productId}/versions")
    @Operation(summary = "Versionen eines Produkts (neueste zuerst).")
    public ResponseEntity<List<ProductVersionView>> versions(
            @PathVariable UUID productId) {
        return ResponseEntity.ok(queryService.listVersions(productId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CVM_ADMIN')")
    @Operation(summary = "Neues Produkt anlegen (CVM_ADMIN).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Produkt angelegt"),
        @ApiResponse(responseCode = "400", description = "Eingabe ungueltig"),
        @ApiResponse(responseCode = "409", description = "Key bereits vergeben")
    })
    public ResponseEntity<ProductView> anlegen(
            @Valid @RequestBody ProductCreateRequest request) {
        ProductView created = catalogService.anlege(new ProductCreateInput(
                request.key(), request.name(), request.description()));
        return ResponseEntity
                .created(URI.create("/api/v1/products/" + created.id()))
                .body(created);
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAuthority('CVM_ADMIN')")
    @Operation(summary = "Produkt-Stammdaten aktualisieren (Name/Beschreibung).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produkt aktualisiert"),
        @ApiResponse(responseCode = "400", description = "Eingabe ungueltig"),
        @ApiResponse(responseCode = "404", description = "Produkt nicht gefunden")
    })
    public ResponseEntity<ProductView> aktualisieren(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductUpdateRequest request) {
        ProductView updated = catalogService.aktualisiere(productId,
                new ProductUpdateInput(request.name(), request.description()));
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{productId}/versions")
    @PreAuthorize("hasAuthority('CVM_ADMIN')")
    @Operation(summary = "Neue Produktversion anlegen (CVM_ADMIN).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Version angelegt"),
        @ApiResponse(responseCode = "400", description = "Eingabe ungueltig"),
        @ApiResponse(responseCode = "404", description = "Produkt nicht gefunden"),
        @ApiResponse(responseCode = "409", description = "Version bereits vorhanden")
    })
    public ResponseEntity<ProductVersionView> anlegenVersion(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductVersionCreateRequest request) {
        ProductVersionView created = catalogService.anlegeVersion(
                productId,
                new ProductVersionCreateInput(
                        request.version(), request.gitCommit(), request.releasedAt()));
        return ResponseEntity
                .created(URI.create("/api/v1/products/" + productId
                        + "/versions/" + created.id()))
                .body(created);
    }
}
