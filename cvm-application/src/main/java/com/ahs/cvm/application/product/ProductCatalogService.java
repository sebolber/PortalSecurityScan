package com.ahs.cvm.application.product;

import com.ahs.cvm.persistence.product.Product;
import com.ahs.cvm.persistence.product.ProductRepository;
import com.ahs.cvm.persistence.product.ProductVersion;
import com.ahs.cvm.persistence.product.ProductVersionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fachlicher Zugriff auf Produkte und deren Versionen.
 * Stellt neben den Lookups auch die Schreib-Use-Cases bereit,
 * die von {@code ProductsController} fuer die Admin-Anlage genutzt werden.
 */
@Service
public class ProductCatalogService {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z0-9-]{2,64}$");

    private final ProductRepository productRepository;
    private final ProductVersionRepository productVersionRepository;

    public ProductCatalogService(
            ProductRepository productRepository,
            ProductVersionRepository productVersionRepository) {
        this.productRepository = productRepository;
        this.productVersionRepository = productVersionRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Product> findeProduktUeberKey(String key) {
        return productRepository.findByKey(key);
    }

    @Transactional(readOnly = true)
    public Optional<ProductVersion> findeProduktVersion(UUID produktId, String version) {
        return productVersionRepository.findByProductIdAndVersion(produktId, version);
    }

    @Transactional
    public Product speichereProdukt(Product produkt) {
        return productRepository.save(produkt);
    }

    @Transactional
    public ProductVersion speichereProduktVersion(ProductVersion produktVersion) {
        return productVersionRepository.save(produktVersion);
    }

    /**
     * Legt ein neues Produkt an. Validiert den Key (Regex {@code ^[a-z0-9-]{2,64}$})
     * und verwirft Kollisionen mit bestehenden Produkten.
     */
    @Transactional
    public ProductView anlege(ProductCreateInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Eingabe darf nicht null sein.");
        }
        String key = normalisiereKey(input.key());
        String name = trim(input.name(), "name");
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "Produkt-Key ungueltig (erwartet ^[a-z0-9-]{2,64}$): " + key);
        }
        if (productRepository.findByKey(key).isPresent()) {
            throw new ProductKeyConflictException(key);
        }
        Product p = productRepository.save(Product.builder()
                .name(name)
                .key(key)
                .description(input.description() == null ? null : input.description().trim())
                .build());
        return ProductView.from(p);
    }

    /**
     * Legt eine neue Produktversion fuer ein bestehendes Produkt an.
     * Wirft {@link ProductNotFoundException} wenn das Produkt fehlt
     * und {@link ProductVersionConflictException} bei Dubletten.
     */
    @Transactional
    public ProductVersionView anlegeVersion(UUID productId, ProductVersionCreateInput input) {
        if (productId == null) {
            throw new IllegalArgumentException("productId darf nicht null sein.");
        }
        if (input == null) {
            throw new IllegalArgumentException("Eingabe darf nicht null sein.");
        }
        String version = trim(input.version(), "version");
        Product produkt = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        if (productVersionRepository.findByProductIdAndVersion(productId, version).isPresent()) {
            throw new ProductVersionConflictException(productId, version);
        }
        ProductVersion saved = productVersionRepository.save(ProductVersion.builder()
                .product(produkt)
                .version(version)
                .gitCommit(input.gitCommit() == null || input.gitCommit().isBlank()
                        ? null
                        : input.gitCommit().trim())
                .releasedAt(input.releasedAt())
                .build());
        return ProductVersionView.from(saved);
    }

    private static String normalisiereKey(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Produkt-Key darf nicht leer sein.");
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Produkt-Key darf nicht leer sein.");
        }
        return trimmed;
    }

    private static String trim(String value, String feldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(feldName + " darf nicht leer sein.");
        }
        return value.trim();
    }

    public record ProductCreateInput(String key, String name, String description) {}

    public record ProductVersionCreateInput(
            String version, String gitCommit, Instant releasedAt) {}
}
