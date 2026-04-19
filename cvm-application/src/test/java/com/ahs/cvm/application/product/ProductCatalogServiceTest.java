package com.ahs.cvm.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.product.ProductCatalogService.ProductCreateInput;
import com.ahs.cvm.application.product.ProductCatalogService.ProductUpdateInput;
import com.ahs.cvm.application.product.ProductCatalogService.ProductVersionCreateInput;
import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.persistence.product.Product;
import com.ahs.cvm.persistence.product.ProductRepository;
import com.ahs.cvm.persistence.product.ProductVersion;
import com.ahs.cvm.persistence.product.ProductVersionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductCatalogServiceTest {

    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private ProductRepository productRepo;
    private ProductVersionRepository versionRepo;
    private ProductCatalogService service;

    @BeforeEach
    void setUp() {
        productRepo = mock(ProductRepository.class);
        versionRepo = mock(ProductVersionRepository.class);
        service = new ProductCatalogService(productRepo, versionRepo);
        TenantContext.set(TENANT);
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

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("anlege: legt Produkt mit key und name an (Happy-Path)")
    void anlegeHappyPath() {
        given(productRepo.findByTenantIdAndKey(TENANT, "portalcore-test"))
                .willReturn(Optional.empty());

        ProductView result = service.anlege(new ProductCreateInput(
                "portalcore-test", "PortalCore-Test", "Kernmodul"));

        assertThat(result.key()).isEqualTo("portalcore-test");
        assertThat(result.name()).isEqualTo("PortalCore-Test");
        assertThat(result.description()).isEqualTo("Kernmodul");
    }

    @Test
    @DisplayName("anlege: trimmt Whitespace im Key")
    void anlegeTrimmtKey() {
        given(productRepo.findByTenantIdAndKey(TENANT, "portalcore-test"))
                .willReturn(Optional.empty());

        ProductView result = service.anlege(new ProductCreateInput(
                "  portalcore-test ", "PortalCore", null));

        assertThat(result.key()).isEqualTo("portalcore-test");
    }

    @Test
    @DisplayName("anlege: wirft ProductKeyConflictException bei Duplikat innerhalb des Mandanten")
    void anlegeKeyKonflikt() {
        Product bestehend = Product.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .key("portalcore-test").name("existing").build();
        given(productRepo.findByTenantIdAndKey(TENANT, "portalcore-test"))
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
    @DisplayName("anlege: ohne Tenant-Kontext wirft IllegalStateException")
    void anlegeOhneTenant() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.anlege(new ProductCreateInput(
                "portalcore-test", "x", null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("anlegeVersion: Happy-Path legt Version an")
    void anlegeVersionHappyPath() {
        UUID productId = UUID.randomUUID();
        Product p = Product.builder()
                .id(productId).tenantId(TENANT)
                .key("portalcore-test").name("PortalCore").build();
        given(productRepo.findById(productId)).willReturn(Optional.of(p));
        given(versionRepo.findByProductIdAndVersionAndDeletedAtIsNull(productId, "1.15.0-test"))
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
                .id(productId).tenantId(TENANT)
                .key("portalcore-test").name("PortalCore").build();
        ProductVersion bestehend = ProductVersion.builder()
                .id(UUID.randomUUID()).tenantId(TENANT)
                .product(p).version("1.14.2-test").build();
        given(productRepo.findById(productId)).willReturn(Optional.of(p));
        given(versionRepo.findByProductIdAndVersionAndDeletedAtIsNull(productId, "1.14.2-test"))
                .willReturn(Optional.of(bestehend));

        assertThatThrownBy(() -> service.anlegeVersion(productId,
                new ProductVersionCreateInput("1.14.2-test", null, null)))
                .isInstanceOf(ProductVersionConflictException.class);
    }

    @Test
    @DisplayName("aktualisiere: Name und Beschreibung werden gespeichert")
    void aktualisiereHappyPath() {
        UUID productId = UUID.randomUUID();
        Product p = Product.builder()
                .id(productId).tenantId(TENANT)
                .key("portalcore-test").name("Old Name")
                .description("Old").build();
        given(productRepo.findById(productId)).willReturn(Optional.of(p));

        ProductView result = service.aktualisiere(productId,
                new ProductUpdateInput("  Neuer Name ", "  Neu "));

        assertThat(result.name()).isEqualTo("Neuer Name");
        assertThat(result.description()).isEqualTo("Neu");
        assertThat(result.key()).isEqualTo("portalcore-test");
    }

    @Test
    @DisplayName("aktualisiere: null-Felder bleiben unveraendert")
    void aktualisiereNullsIgnorieren() {
        UUID productId = UUID.randomUUID();
        Product p = Product.builder()
                .id(productId).tenantId(TENANT)
                .key("portalcore-test").name("Name")
                .description("Desc").build();
        given(productRepo.findById(productId)).willReturn(Optional.of(p));

        ProductView result = service.aktualisiere(productId,
                new ProductUpdateInput(null, null));
        assertThat(result.name()).isEqualTo("Name");
        assertThat(result.description()).isEqualTo("Desc");
    }

    @Test
    @DisplayName("aktualisiere: leerer Name wirft IllegalArgument")
    void aktualisiereNameLeer() {
        UUID productId = UUID.randomUUID();
        Product p = Product.builder()
                .id(productId).tenantId(TENANT)
                .key("portalcore-test").name("Name").build();
        given(productRepo.findById(productId)).willReturn(Optional.of(p));

        assertThatThrownBy(() -> service.aktualisiere(productId,
                new ProductUpdateInput("   ", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("aktualisiere: unbekannte Produkt-Id wirft ProductNotFoundException")
    void aktualisiereUnbekannt() {
        UUID productId = UUID.randomUUID();
        given(productRepo.findById(productId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.aktualisiere(productId,
                new ProductUpdateInput("x", null)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("aktualisiere: leere Beschreibung wird zu null normalisiert")
    void aktualisiereBeschreibungLeer() {
        UUID productId = UUID.randomUUID();
        Product p = Product.builder()
                .id(productId).tenantId(TENANT)
                .key("portalcore-test").name("Name")
                .description("Old").build();
        given(productRepo.findById(productId)).willReturn(Optional.of(p));

        ProductView result = service.aktualisiere(productId,
                new ProductUpdateInput(null, "   "));
        assertThat(result.description()).isNull();
    }

    @Test
    @DisplayName("aktualisiere: Produkt aus anderem Mandanten -> ProductNotFoundException")
    void aktualisiereFremderMandant() {
        UUID productId = UUID.randomUUID();
        Product p = Product.builder()
                .id(productId).tenantId(UUID.randomUUID())
                .key("portalcore-test").name("Name").build();
        given(productRepo.findById(productId)).willReturn(Optional.of(p));

        assertThatThrownBy(() -> service.aktualisiere(productId,
                new ProductUpdateInput("x", null)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("loesche: setzt deletedAt auf Jetzt")
    void loescheHappyPath() {
        UUID productId = UUID.randomUUID();
        Product p = Product.builder()
                .id(productId).tenantId(TENANT)
                .key("portalcore-test").name("Name").build();
        given(productRepo.findById(productId)).willReturn(Optional.of(p));

        service.loesche(productId);

        assertThat(p.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("loesche: bereits geloeschte Produkte werden nicht erneut markiert")
    void loescheIdempotent() {
        UUID productId = UUID.randomUUID();
        Instant gesetzt = Instant.parse("2026-01-01T00:00:00Z");
        Product p = Product.builder()
                .id(productId).tenantId(TENANT)
                .key("portalcore-test").name("Name")
                .deletedAt(gesetzt).build();
        given(productRepo.findById(productId)).willReturn(Optional.of(p));

        service.loesche(productId);

        assertThat(p.getDeletedAt()).isEqualTo(gesetzt);
    }

    @Test
    @DisplayName("loesche: unbekannte Id wirft ProductNotFoundException")
    void loescheUnbekannt() {
        UUID productId = UUID.randomUUID();
        given(productRepo.findById(productId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.loesche(productId))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("loesche: Produkt aus anderem Mandanten -> ProductNotFoundException")
    void loescheFremderMandant() {
        UUID productId = UUID.randomUUID();
        Product p = Product.builder()
                .id(productId).tenantId(UUID.randomUUID())
                .key("portalcore-test").name("Name").build();
        given(productRepo.findById(productId)).willReturn(Optional.of(p));

        assertThatThrownBy(() -> service.loesche(productId))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("loescheVersion: setzt deletedAt und bewahrt Zeitpunkt danach")
    void loescheVersionHappyPath() {
        UUID productId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Product p = Product.builder().id(productId).tenantId(TENANT).key("p").name("P").build();
        ProductVersion v = ProductVersion.builder()
                .id(versionId).tenantId(TENANT).product(p).version("1.0.0").build();
        given(versionRepo.findById(versionId)).willReturn(Optional.of(v));

        service.loescheVersion(productId, versionId);

        assertThat(v.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("loescheVersion: bereits geloeschte Version bleibt unveraendert (Idempotenz)")
    void loescheVersionIdempotent() {
        UUID productId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Product p = Product.builder().id(productId).tenantId(TENANT).key("p").name("P").build();
        Instant vorher = Instant.parse("2026-01-01T00:00:00Z");
        ProductVersion v = ProductVersion.builder()
                .id(versionId).tenantId(TENANT).product(p).version("1.0.0").deletedAt(vorher).build();
        given(versionRepo.findById(versionId)).willReturn(Optional.of(v));

        service.loescheVersion(productId, versionId);

        assertThat(v.getDeletedAt()).isEqualTo(vorher);
    }

    @Test
    @DisplayName("loescheVersion: unbekannte versionId -> ProductVersionNotFoundException")
    void loescheVersionUnbekannt() {
        UUID productId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        given(versionRepo.findById(versionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.loescheVersion(productId, versionId))
                .isInstanceOf(ProductVersionNotFoundException.class);
    }

    @Test
    @DisplayName("loescheVersion: Version gehoert zu anderem Produkt -> ProductVersionNotFoundException")
    void loescheVersionFalschesProdukt() {
        UUID productId = UUID.randomUUID();
        UUID andererId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Product p = Product.builder().id(andererId).tenantId(TENANT).key("p").name("P").build();
        ProductVersion v = ProductVersion.builder()
                .id(versionId).tenantId(TENANT).product(p).version("1.0.0").build();
        given(versionRepo.findById(versionId)).willReturn(Optional.of(v));

        assertThatThrownBy(() -> service.loescheVersion(productId, versionId))
                .isInstanceOf(ProductVersionNotFoundException.class);
    }

    @Test
    @DisplayName("loescheVersion: null-Parameter -> IllegalArgumentException")
    void loescheVersionNull() {
        assertThatThrownBy(() -> service.loescheVersion(null, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.loescheVersion(UUID.randomUUID(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
