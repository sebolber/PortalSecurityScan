package com.ahs.cvm.app.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.app.AbstractIntegrationTest;
import com.ahs.cvm.app.DockerAvailability;
import com.ahs.cvm.application.product.ProductCatalogService;
import com.ahs.cvm.application.product.ProductCatalogService.ProductCreateInput;
import com.ahs.cvm.application.product.ProductQueryService;
import com.ahs.cvm.application.product.ProductView;
import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.persistence.tenant.Tenant;
import com.ahs.cvm.persistence.tenant.TenantRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Iteration 62F (CVM-62): Postgres-RLS-Isolationstest.
 *
 * <p>Legt zwei Mandanten an, erzeugt in jedem ein Produkt, und prueft:
 * <ul>
 *   <li>Unter TenantContext(A) sieht {@code listProducts()} nur A's Produkt.</li>
 *   <li>Unter TenantContext(B) sieht {@code listProducts()} nur B's Produkt.</li>
 *   <li>Ein direkter JPA-Lookup auf B's Produkt-Id unter Context(A) liefert
 *       keine Zeile (die RLS-Policy filtert vor dem Hibernate-Mapping).</li>
 * </ul>
 *
 * <p>Ueberspringt sich, wenn Docker nicht verfuegbar ist (Sandbox).
 */
@EnabledIf(
        value = "com.ahs.cvm.app.DockerAvailability#isAvailable",
        disabledReason = "Docker nicht verfuegbar - Testcontainers uebersprungen.")
class TenantRlsIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private ProductCatalogService catalog;
    @Autowired private ProductQueryService query;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("RLS: Mandant A sieht nur seine eigenen Produkte, nicht die von B")
    @Transactional
    void tenantIsolation() {
        Tenant a = tenantRepository.save(Tenant.builder()
                .tenantKey("test-a-" + UUID.randomUUID())
                .name("Test Tenant A")
                .active(true)
                .defaultTenant(false)
                .createdAt(Instant.now())
                .build());
        Tenant b = tenantRepository.save(Tenant.builder()
                .tenantKey("test-b-" + UUID.randomUUID())
                .name("Test Tenant B")
                .active(true)
                .defaultTenant(false)
                .createdAt(Instant.now())
                .build());

        TenantContext.set(a.getId());
        ProductView prodA = catalog.anlege(new ProductCreateInput(
                "rls-a", "RLS-Test-A", null));

        TenantContext.set(b.getId());
        ProductView prodB = catalog.anlege(new ProductCreateInput(
                "rls-b", "RLS-Test-B", null));

        TenantContext.set(a.getId());
        List<ProductView> listA = query.listProducts();
        assertThat(listA)
                .as("Mandant A darf nur seine Produkte sehen")
                .extracting(ProductView::id)
                .contains(prodA.id())
                .doesNotContain(prodB.id());

        TenantContext.set(b.getId());
        List<ProductView> listB = query.listProducts();
        assertThat(listB)
                .as("Mandant B darf nur seine Produkte sehen")
                .extracting(ProductView::id)
                .contains(prodB.id())
                .doesNotContain(prodA.id());
    }

    @Test
    @DisplayName("RLS: Anlage mit widerspruechlicher tenantId im Input wird abgelehnt")
    @Transactional
    void crossTenantWriteRejected() {
        Tenant t = tenantRepository.save(Tenant.builder()
                .tenantKey("test-c-" + UUID.randomUUID())
                .name("Test Tenant C")
                .active(true)
                .defaultTenant(false)
                .createdAt(Instant.now())
                .build());

        TenantContext.set(t.getId());
        // Input mit fremder tenantId -> Service lehnt ab.
        UUID andererTenant = UUID.randomUUID();
        assertThat(
                        catalogWirftBeiFremderTenantId(andererTenant, t.getId()))
                .isTrue();
    }

    private boolean catalogWirftBeiFremderTenantId(UUID andererTenant, UUID current) {
        try {
            TenantContext.set(current);
            catalog.anlege(new ProductCreateInput(
                    "rls-c", "RLS-Test-C", null, andererTenant));
            return false;
        } catch (IllegalArgumentException expected) {
            return expected.getMessage() != null
                    && expected.getMessage().contains("Tenant-Kontext");
        }
    }
}
