package com.ahs.cvm.api.vex;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.vex.VexExporter;
import com.ahs.cvm.application.vex.VexImporter;
import com.ahs.cvm.application.vex.VexStatement;
import com.ahs.cvm.application.vex.VexStatus;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = VexController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(VexExceptionHandler.class)
class VexControllerWebTest {

    private static final UUID PV = UUID.fromString(
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired MockMvc mockMvc;
    @MockBean VexExporter exporter;
    @MockBean VexImporter importer;

    @Test
    @DisplayName("GET /vex/{id}: CycloneDX JSON mit Header")
    void cyclonedx() throws Exception {
        given(exporter.export(eq(PV), eq("cyclonedx")))
                .willReturn("{\"bomFormat\":\"CycloneDX\"}");
        mockMvc.perform(get("/api/v1/vex/" + PV))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("X-VEX-Format", "cyclonedx"))
                .andExpect(content().string("{\"bomFormat\":\"CycloneDX\"}"));
    }

    @Test
    @DisplayName("GET /vex/{id}?format=csaf: CSAF JSON")
    void csaf() throws Exception {
        given(exporter.export(eq(PV), eq("csaf")))
                .willReturn("{\"document\":{\"category\":\"csaf_vex\"}}");
        mockMvc.perform(get("/api/v1/vex/" + PV).param("format", "csaf"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-VEX-Format", "csaf"));
    }

    @Test
    @DisplayName("GET /vex/{id}?format=xml: 400 unbekanntes Format")
    void unbekanntesFormat() throws Exception {
        mockMvc.perform(get("/api/v1/vex/" + PV).param("format", "xml"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("vex_bad_request"));
    }

    @Test
    @DisplayName("GET /vex/{id}: unbekannte productVersionId -> 404")
    void unbekanntePv() throws Exception {
        willThrow(new IllegalArgumentException(
                "Unbekannte productVersionId: " + PV))
                .given(exporter).export(any(), any());
        mockMvc.perform(get("/api/v1/vex/" + PV))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("vex_not_found"));
    }

    @Test
    @DisplayName("POST /vex/import (JSON): 200 mit Statements")
    void importJsonOk() throws Exception {
        given(importer.parse(any())).willReturn(new VexImporter.Parsed(
                List.of(new VexStatement("CVE-X", "pkg:m", VexStatus.NOT_AFFECTED,
                        "component_not_present", "", null, List.of())),
                List.of(), List.of()));
        mockMvc.perform(post("/api/v1/vex/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bomFormat\":\"CycloneDX\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statements[0].cveKey").value("CVE-X"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("POST /vex/import (JSON): Fehler -> 422")
    void importJsonFehler() throws Exception {
        given(importer.parse(any())).willReturn(new VexImporter.Parsed(
                List.of(), List.of(),
                List.of("Nur CycloneDX wird unterstuetzt.")));
        mockMvc.perform(post("/api/v1/vex/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bomFormat\":\"SPDX\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0]").value(
                        org.hamcrest.Matchers.containsString("CycloneDX")));
    }

    @Test
    @DisplayName("POST /vex/import (multipart): 200")
    void importMultipart() throws Exception {
        given(importer.parse(any())).willReturn(new VexImporter.Parsed(
                List.of(), List.of("Hinweis"), List.of()));
        MockMultipartFile file = new MockMultipartFile(
                "file", "vex.json", "application/json",
                "{\"bomFormat\":\"CycloneDX\"}".getBytes());
        mockMvc.perform(multipart("/api/v1/vex/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings[0]").value("Hinweis"));
    }

    @Test
    @DisplayName("POST /vex/import (multipart): leere Datei -> 400")
    void importMultipartLeer() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vex.json", "application/json", new byte[0]);
        mockMvc.perform(multipart("/api/v1/vex/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("vex_bad_request"));
    }
}
