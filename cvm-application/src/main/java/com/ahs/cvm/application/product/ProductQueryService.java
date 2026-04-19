package com.ahs.cvm.application.product;

import com.ahs.cvm.application.tenant.TenantContext;
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
 *
 * <p>Iteration 62A (CVM-62): Filtert die Ergebnisse auf den im
 * {@link TenantContext} gesetzten Mandanten. Ohne Tenant-Kontext wird
 * eine leere Liste geliefert, damit versehentliche Cross-Tenant-Reads
 * nie sichtbar werden.
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
        return TenantContext.current()
                .map(tenantId -> productRepository
                        .findByTenantIdAndDeletedAtIsNullOrderByKeyAsc(tenantId)
                        .stream()
                        .map(ProductView::from)
                        .toList())
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<ProductVersionView> listVersions(UUID productId) {
        UUID tenant = TenantContext.current().orElse(null);
        return versionRepository.findByProductIdAndDeletedAtIsNull(productId).stream()
                .filter(v -> tenant == null || tenant.equals(v.getTenantId()))
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
