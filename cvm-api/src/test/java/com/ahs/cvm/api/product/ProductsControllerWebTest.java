package com.ahs.cvm.api.product;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.product.ProductQueryService;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = ProductsController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class ProductsControllerWebTest {

    private static final UUID P1 = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    @Autowired MockMvc mockMvc;
    @MockBean ProductQueryService service;

    @Test
    @DisplayName("GET /api/v1/products: Liste mit key/name")
    void list() throws Exception {
        given(service.listProducts()).willReturn(List.of(
                new ProductView(P1, "PortalCore", "PortalCore", "Kernmodul"),
                new ProductView(UUID.randomUUID(), "SmileKH", "SmileKH", null)));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("PortalCore"))
                .andExpect(jsonPath("$[1].key").value("SmileKH"));
    }

    @Test
    @DisplayName("GET /{id}/versions: neueste zuerst")
    void versions() throws Exception {
        given(service.listVersions(P1)).willReturn(List.of(
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
}
