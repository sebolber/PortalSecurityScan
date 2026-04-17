package com.ahs.cvm.application.product;

import com.ahs.cvm.persistence.product.Product;
import com.ahs.cvm.persistence.product.ProductRepository;
import com.ahs.cvm.persistence.product.ProductVersion;
import com.ahs.cvm.persistence.product.ProductVersionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fachlicher Zugriff auf Produkte und deren Versionen. Skelett fuer
 * Iteration 01. Echte Use-Cases (SBOM-Upload, Deployments) folgen ab Iteration 02.
 */
@Service
public class ProductCatalogService {

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
}
