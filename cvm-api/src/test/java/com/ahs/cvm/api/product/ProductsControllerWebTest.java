package com.ahs.cvm.api.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.product.ProductCatalogService;
import com.ahs.cvm.application.product.ProductKeyConflictException;
import com.ahs.cvm.application.product.ProductNotFoundException;
import com.ahs.cvm.application.product.ProductQueryService;
import com.ahs.cvm.application.product.ProductVersionConflictException;
import com.ahs.cvm.application.product.ProductVersionView;
import com.ahs.cvm.application.product.ProductView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = ProductsController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(ProductsExceptionHandler.class)
class ProductsControllerWebTest {

    private static final UUID P1 = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    @Autowired MockMvc mockMvc;
    @MockBean ProductQueryService queryService;
    @MockBean ProductCatalogService catalogService;

    @Test
    @DisplayName("GET /api/v1/products: Liste mit key/name")
    void list() throws Exception {
        given(queryService.listProducts()).willReturn(List.of(
                new ProductView(P1, "PortalCore", "PortalCore", "Kernmodul", null),
                new ProductView(UUID.randomUUID(), "SmileKH", "SmileKH", null, null)));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("PortalCore"))
                .andExpect(jsonPath("$[1].key").value("SmileKH"));
    }

    @Test
    @DisplayName("GET /{id}/versions: neueste zuerst")
    void versions() throws Exception {
        given(queryService.listVersions(P1)).willReturn(List.of(
                new ProductVersionView(UUID.randomUUID(), P1,
                        "1.14.2-test", "a3f9beef",
                        Instant.parse("2026-04-01T00:00:00Z")),
                new ProductVersionView(UUID.randomUUID(), P1,
                        "1.13.0-test", "cafebabe",
                        Instant.parse("2026-01-15T00:00:00Z"))));

        mockMvc.perform(get("/api/v1/products/" + P1 + "/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value("1.14.2-test"))
                .andExpect(jsonPath("$[1].version").value("1.13.0-test"));
    }

    @Test
    @DisplayName("POST /api/v1/products: 201 Created mit Location-Header")
    void anlegenHappyPath() throws Exception {
        UUID neueId = UUID.randomUUID();
        given(catalogService.anlege(any()))
                .willReturn(new ProductView(neueId, "portalcore-test",
                        "PortalCore-Test", "Test-Komponente", null));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "key": "portalcore-test",
                                  "name": "PortalCore-Test",
                                  "description": "Test-Komponente"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/products/" + neueId))
                .andExpect(jsonPath("$.key").value("portalcore-test"));
    }

    @Test
    @DisplayName("POST /api/v1/products: 409 bei doppeltem Key")
    void anlegenKeyKonflikt() throws Exception {
        willThrow(new ProductKeyConflictException("portalcore-test"))
                .given(catalogService).anlege(any());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"portalcore-test","name":"X"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("product_key_conflict"));
    }

    @Test
    @DisplayName("POST /api/v1/products: 400 bei invalidem Key-Format")
    void anlegenKeyUngueltig() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"Invalid Key!","name":"X"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/products: 400 wenn name fehlt")
    void anlegenNameFehlt() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"portalcore-test"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /{id}/versions: 201 Created")
    void anlegenVersion() throws Exception {
        UUID versionId = UUID.randomUUID();
        given(catalogService.anlegeVersion(eq(P1), any()))
                .willReturn(new ProductVersionView(
                        versionId, P1, "1.15.0-test", "deadbeef",
                        Instant.parse("2026-04-15T00:00:00Z")));

        mockMvc.perform(post("/api/v1/products/" + P1 + "/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": "1.15.0-test",
                                  "gitCommit": "deadbeef",
                                  "releasedAt": "2026-04-15T00:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/products/" + P1 + "/versions/" + versionId))
                .andExpect(jsonPath("$.version").value("1.15.0-test"));
    }

    @Test
    @DisplayName("POST /{id}/versions: 404 wenn Produkt fehlt")
    void anlegenVersionProduktUnbekannt() throws Exception {
        willThrow(new ProductNotFoundException(P1))
                .given(catalogService).anlegeVersion(any(), any());

        mockMvc.perform(post("/api/v1/products/" + P1 + "/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":"1.0.0"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("product_not_found"));
    }

    @Test
    @DisplayName("POST /{id}/versions: 409 bei Duplikat")
    void anlegenVersionKonflikt() throws Exception {
        willThrow(new ProductVersionConflictException(P1, "1.14.2-test"))
                .given(catalogService).anlegeVersion(any(), any());

        mockMvc.perform(post("/api/v1/products/" + P1 + "/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":"1.14.2-test"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("product_version_conflict"));
    }
}
