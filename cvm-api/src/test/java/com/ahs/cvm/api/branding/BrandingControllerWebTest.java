package com.ahs.cvm.api.branding;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.branding.BrandingAssetService;
import com.ahs.cvm.application.branding.BrandingAssetService.AssetKind;
import com.ahs.cvm.application.branding.BrandingAssetView;
import com.ahs.cvm.application.branding.BrandingHistoryEntry;
import com.ahs.cvm.application.branding.BrandingService;
import com.ahs.cvm.application.branding.BrandingService.ContrastViolationException;
import com.ahs.cvm.application.branding.BrandingService.UnknownBrandingVersionException;
import com.ahs.cvm.application.branding.BrandingView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = BrandingController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class BrandingControllerWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;
    @MockBean BrandingService service;
    @MockBean BrandingAssetService assetService;

    @Test
    @DisplayName("GET /api/v1/theme: liefert aktive Branding-Konfiguration")
    void getAktivesBranding() throws Exception {
        given(service.loadForCurrentTenant()).willReturn(new BrandingView(
                "#006ec7", "#ffffff", "#887d75",
                "Fira Sans", "Fira Code",
                "CVE-Relevance-Manager",
                null, "adesso health solutions", null, null, 2));

        mockMvc.perform(get("/api/v1/theme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#006ec7"))
                .andExpect(jsonPath("$.fontFamilyName").value("Fira Sans"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/theme: gueltiger Request gibt neue Version zurueck")
    void putErfolgreich() throws Exception {
        given(service.updateForCurrentTenant(any(), anyString()))
                .willReturn(new BrandingView(
                        "#003a68", "#ffffff", null,
                        "Fira Sans", null, "CVM", null, "adesso", null, null, 3));

        Map<String, Object> body = Map.of(
                "primaryColor", "#003a68",
                "primaryContrastColor", "#ffffff",
                "fontFamilyName", "Fira Sans",
                "expectedVersion", 2);

        mockMvc.perform(put("/api/v1/admin/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#003a68"))
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/theme: Kontrastverletzung liefert 422")
    void putKontrast() throws Exception {
        willThrow(new ContrastViolationException("Kontrast zu niedrig"))
                .given(service).updateForCurrentTenant(any(), anyString());

        Map<String, Object> body = Map.of(
                "primaryColor", "#cccccc",
                "primaryContrastColor", "#ffffff",
                "fontFamilyName", "Fira Sans",
                "expectedVersion", 1);

        mockMvc.perform(put("/api/v1/admin/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("contrast_violation"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/theme/assets: Logo-Upload liefert 201 + Location")
    void uploadLogo() throws Exception {
        UUID assetId = UUID.randomUUID();
        BrandingAssetView saved = new BrandingAssetView(
                assetId, UUID.randomUUID(), "LOGO", "image/svg+xml",
                42, "abc", new byte[] {1, 2, 3});
        given(assetService.upload(
                        any(AssetKind.class), anyString(), any(byte[].class), anyString()))
                .willReturn(saved);

        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.svg", "image/svg+xml",
                "<svg></svg>".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/theme/assets")
                        .file(file)
                        .param("kind", "LOGO"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/theme/assets/" + assetId))
                .andExpect(jsonPath("$.kind").value("LOGO"))
                .andExpect(jsonPath("$.sha256").value("abc"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/theme/history: liefert History-Liste")
    void getHistory() throws Exception {
        BrandingHistoryEntry entry = new BrandingHistoryEntry(
                3, "#006ec7", "#ffffff", "#887d75",
                "Fira Sans", "Fira Code", "CVE-Relevance-Manager",
                null, "adesso", null, null,
                Instant.parse("2026-04-17T10:00:00Z"), "admin",
                Instant.parse("2026-04-17T10:05:00Z"), "admin");
        given(service.history(anyInt())).willReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/admin/theme/history?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value(3))
                .andExpect(jsonPath("$[0].primaryColor").value("#006ec7"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/theme/rollback/{version}: Happy-Path liefert neue Version")
    void rollbackHappy() throws Exception {
        given(service.rollbackForCurrentTenant(eq(3), anyString()))
                .willReturn(new BrandingView(
                        "#006ec7", "#ffffff", null,
                        "Fira Sans", null, "CVM", null, "adesso", null, null, 8));

        mockMvc.perform(post("/api/v1/admin/theme/rollback/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#006ec7"))
                .andExpect(jsonPath("$.version").value(8));
    }

    @Test
    @DisplayName("POST /api/v1/admin/theme/rollback/{version}: unbekannte Version liefert 404")
    void rollbackUnbekannt() throws Exception {
        willThrow(new UnknownBrandingVersionException("Unbekannt: 99"))
                .given(service).rollbackForCurrentTenant(eq(99), anyString());

        mockMvc.perform(post("/api/v1/admin/theme/rollback/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("branding_version_unknown"));
    }

    @Test
    @DisplayName("GET /api/v1/theme/assets/{id}: liefert Bytes mit ETag")
    void downloadAsset() throws Exception {
        UUID assetId = UUID.randomUUID();
        BrandingAssetView view = new BrandingAssetView(
                assetId, UUID.randomUUID(), "LOGO", "image/svg+xml",
                5, "xyz", "<svg/>".getBytes());
        given(assetService.findById(assetId)).willReturn(Optional.of(view));

        mockMvc.perform(get("/api/v1/theme/assets/" + assetId))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"xyz\""))
                .andExpect(header().string("Cache-Control", "public, max-age=3600"));
    }
}
