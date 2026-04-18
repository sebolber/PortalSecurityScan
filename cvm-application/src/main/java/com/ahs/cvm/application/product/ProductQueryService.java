package com.ahs.cvm.application.product;

import com.ahs.cvm.persistence.product.ProductRepository;
import com.ahs.cvm.persistence.product.ProductVersion;
import com.ahs.cvm.persistence.product.ProductVersionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-Service fuer Produkte und deren Versionen
 * (Iteration 26, CVM-57).
 */
@Service
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductVersionRepository versionRepository;

    public ProductQueryService(
            ProductRepository productRepository,
            ProductVersionRepository versionRepository) {
        this.productRepository = productRepository;
        this.versionRepository = versionRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductView> listProducts() {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        p -> p.getKey() == null ? "" : p.getKey(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(ProductView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductVersionView> listVersions(UUID productId) {
        return versionRepository.findByProductId(productId).stream()
                .sorted(Comparator
                        .comparing(
                                (ProductVersion v) -> v.getReleasedAt() == null
                                        ? java.time.Instant.EPOCH
                                        : v.getReleasedAt())
                        .reversed())
                .map(ProductVersionView::from)
                .toList();
    }
}
