package com.ahs.cvm.persistence.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.persistence.AbstractPersistenceIntegrationsTest;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

@EnabledIf(
        value = "com.ahs.cvm.persistence.support.DockerAvailability#isAvailable",
        disabledReason = "Docker-Daemon nicht erreichbar")
class ProductRepositoryIntegrationsTest extends AbstractPersistenceIntegrationsTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductVersionRepository productVersionRepository;

    @Test
    @DisplayName("Product: save setzt id und createdAt via @PrePersist")
    void produktSpeichernSetztTechnischeFelder() {
        Product portalcore = Product.builder()
                .name("PortalCore-Test")
                .key("portalcore-test")
                .description("Referenz-Produkt")
                .build();

        Product gespeichert = productRepository.save(portalcore);

        assertThat(gespeichert.getId()).isNotNull();
        assertThat(gespeichert.getCreatedAt()).isNotNull();
        assertThat(gespeichert.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("ProductVersion: findByProductIdAndVersion liefert eindeutigen Treffer")
    void produktVersionLookup() {
        Product produkt = productRepository.save(
                Product.builder().name("SmileKH-Test").key("smile-test").build());

        ProductVersion version = productVersionRepository.save(
                ProductVersion.builder()
                        .product(produkt)
                        .version("1.14.2-test")
                        .gitCommit("a3f9beef")
                        .build());

        assertThat(productVersionRepository
                        .findByProductIdAndVersion(produkt.getId(), "1.14.2-test"))
                .hasValueSatisfying(v -> assertThat(v.getId()).isEqualTo(version.getId()));
    }
}
