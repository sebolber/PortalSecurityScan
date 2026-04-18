package com.ahs.cvm.api.branding;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.branding.BrandingService;
import com.ahs.cvm.application.branding.BrandingService.ContrastViolationException;
import com.ahs.cvm.application.branding.BrandingView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = BrandingController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class BrandingControllerWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;
    @MockBean BrandingService service;

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
}
