package com.ahs.cvm.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.product.ProductCatalogService.ProductCreateInput;
import com.ahs.cvm.application.product.ProductCatalogService.ProductVersionCreateInput;
import com.ahs.cvm.persistence.product.Product;
import com.ahs.cvm.persistence.product.ProductRepository;
import com.ahs.cvm.persistence.product.ProductVersion;
import com.ahs.cvm.persistence.product.ProductVersionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductCatalogServiceTest {

    private ProductRepository productRepo;
    private ProductVersionRepository versionRepo;
    private ProductCatalogService service;

    @BeforeEach
    void setUp() {
        productRepo = mock(ProductRepository.class);
        versionRepo = mock(ProductVersionRepository.class);
        service = new ProductCatalogService(productRepo, versionRepo);
        given(productRepo.save(any(Product.class))).willAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            if (p.getCreatedAt() == null) {
                p.setCreatedAt(Instant.now());
            }
            return p;
        });
        given(versionRepo.save(any(ProductVersion.class))).willAnswer(inv -> {
            ProductVersion v = inv.getArgument(0);
            if (v.getId() == null) {
                v.setId(UUID.randomUUID());
            }
            if (v.getCreatedAt() == null) {
                v.setCreatedAt(Instant.now());
            }
            return v;
        });
    }

    @Test
    @DisplayName("anlege: legt Produkt mit key und name an (Happy-Path)")
    void anlegeHappyPath() {
        given(productRepo.findByKey("portalcore-test")).willReturn(Optional.empty());

        ProductView result = service.anlege(new ProductCreateInput(
                "portalcore-test", "PortalCore-Test", "Kernmodul"));

        assertThat(result.key()).isEqualTo("portalcore-test");
        assertThat(result.name()).isEqualTo("PortalCore-Test");
        assertThat(result.description()).isEqualTo("Kernmodul");
    }

    @Test
    @DisplayName("anlege: trimmt Whitespace im Key")
    void anlegeTrimmtKey() {
        given(productRepo.findByKey("portalcore-test")).willReturn(Optional.empty());

        ProductView result = service.anlege(new ProductCreateInput(
                "  portalcore-test ", "PortalCore", null));

        assertThat(result.key()).isEqualTo("portalcore-test");
    }

    @Test
    @DisplayName("anlege: wirft ProductKeyConflictException bei Duplikat")
    void anlegeKeyKonflikt() {
        Product bestehend = Product.builder()
                .id(UUID.randomUUID())
                .key("portalcore-test").name("existing").build();
        given(productRepo.findByKey("portalcore-test"))
                .willReturn(Optional.of(bestehend));

        assertThatThrownBy(() -> service.anlege(new ProductCreateInput(
                "portalcore-test", "x", null)))
                .isInstanceOf(ProductKeyConflictException.class);
    }

    @Test
    @DisplayName("anlege: wirft IllegalArgument bei invalidem Key-Format")
    void anlegeKeyRegex() {
        assertThatThrownBy(() -> service.anlege(new ProductCreateInput(
                "Invalid Key!", "X", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ungueltig");
    }

    @Test
    @DisplayName("anlege: wirft IllegalArgument bei leerem Name")
    void anlegeNameLeer() {
        assertThatThrownBy(() -> service.anlege(new ProductCreateInput(
                "portalcore-test", "  ", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("anlegeVersion: Happy-Path legt Version an")
    void anlegeVersionHappyPath() {
        UUID productId = UUID.randomUUID();
        Product p = Product.builder()
                .id(productId).key("portalcore-test").name("PortalCore").build();
        given(productRepo.findById(productId)).willReturn(Optional.of(p));
        given(versionRepo.findByProductIdAndVersion(productId, "1.15.0-test"))
                .willReturn(Optional.empty());

        ProductVersionView result = service.anlegeVersion(productId,
                new ProductVersionCreateInput(
                        "1.15.0-test", "deadbeef",
                        Instant.parse("2026-04-15T00:00:00Z")));

        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.version()).isEqualTo("1.15.0-test");
        assertThat(result.gitCommit()).isEqualTo("deadbeef");
    }

    @Test
    @DisplayName("anlegeVersion: wirft ProductNotFoundException wenn Produkt fehlt")
    void anlegeVersionProduktFehlt() {
        UUID productId = UUID.randomUUID();
        given(productRepo.findById(productId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.anlegeVersion(productId,
                new ProductVersionCreateInput("1.0.0", null, null)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("anlegeVersion: wirft ProductVersionConflictException bei Duplikat")
    void anlegeVersionKonflikt() {
        UUID productId = UUID.randomUUID();
        Product p = Product.builder()
                .id(productId).key("portalcore-test").name("PortalCore").build();
        ProductVersion bestehend = ProductVersion.builder()
                .id(UUID.randomUUID()).product(p).version("1.14.2-test").build();
        given(productRepo.findById(productId)).willReturn(Optional.of(p));
        given(versionRepo.findByProductIdAndVersion(productId, "1.14.2-test"))
                .willReturn(Optional.of(bestehend));

        assertThatThrownBy(() -> service.anlegeVersion(productId,
                new ProductVersionCreateInput("1.14.2-test", null, null)))
                .isInstanceOf(ProductVersionConflictException.class);
    }
}
