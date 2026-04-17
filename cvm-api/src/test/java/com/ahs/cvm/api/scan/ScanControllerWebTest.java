package com.ahs.cvm.api.scan;

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

import com.ahs.cvm.application.scan.SbomSchemaException;
import com.ahs.cvm.application.scan.ScanAlreadyIngestedException;
import com.ahs.cvm.application.scan.ScanIngestService;
import com.ahs.cvm.application.scan.ScanUploadResponse;
import java.util.Optional;
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
        controllers = ScanController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(ScanExceptionHandler.class)
class ScanControllerWebTest {

    private static final UUID PRODUKT_VERSION_ID = UUID.fromString(
            "11111111-1111-1111-1111-111111111111");
    private static final UUID UMGEBUNG_ID = UUID.fromString(
            "22222222-2222-2222-2222-222222222222");

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ScanIngestService scanIngestService;

    @Test
    @DisplayName("POST /api/v1/scans (multipart): liefert 202 Accepted mit Location-Header")
    void uploadHappyPath() throws Exception {
        UUID scanId = UUID.randomUUID();
        given(scanIngestService.uploadAkzeptieren(
                        eq(PRODUKT_VERSION_ID), eq(UMGEBUNG_ID), eq("trivy"), any()))
                .willReturn(new ScanUploadResponse(scanId, "/api/v1/scans/" + scanId));

        MockMultipartFile sbom = new MockMultipartFile(
                "sbom",
                "klein.json",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"bomFormat\":\"CycloneDX\"}".getBytes());

        mockMvc.perform(multipart("/api/v1/scans")
                        .file(sbom)
                        .param("productVersionId", PRODUKT_VERSION_ID.toString())
                        .param("environmentId", UMGEBUNG_ID.toString())
                        .param("scanner", "trivy"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/v1/scans/" + scanId))
                .andExpect(jsonPath("$.scanId").value(scanId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/scans: 409 Conflict bei ScanAlreadyIngestedException")
    void uploadDuplikat() throws Exception {
        UUID bestehend = UUID.randomUUID();
        willThrow(new ScanAlreadyIngestedException(bestehend))
                .given(scanIngestService)
                .uploadAkzeptieren(any(), any(), any(), any());

        MockMultipartFile sbom = new MockMultipartFile(
                "sbom", "klein.json", MediaType.APPLICATION_JSON_VALUE, "{}".getBytes());

        mockMvc.perform(multipart("/api/v1/scans")
                        .file(sbom)
                        .param("productVersionId", PRODUKT_VERSION_ID.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.existingScanId").value(bestehend.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/scans: 400 Bad Request bei Schema-Fehler")
    void uploadSchemaFehler() throws Exception {
        willThrow(new SbomSchemaException("bomFormat fehlt"))
                .given(scanIngestService)
                .uploadAkzeptieren(any(), any(), any(), any());

        MockMultipartFile sbom = new MockMultipartFile(
                "sbom", "invalid.json", MediaType.APPLICATION_JSON_VALUE, "{}".getBytes());

        mockMvc.perform(multipart("/api/v1/scans")
                        .file(sbom)
                        .param("productVersionId", PRODUKT_VERSION_ID.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("sbom_schema_error"));
    }

    @Test
    @DisplayName("GET /api/v1/scans/{id}: 404 bei unbekanntem Scan")
    void statusNichtGefunden() throws Exception {
        UUID id = UUID.randomUUID();
        given(scanIngestService.zusammenfassung(id)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/scans/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/scans (raw JSON): liefert 202 Accepted")
    void uploadRawJson() throws Exception {
        UUID scanId = UUID.randomUUID();
        given(scanIngestService.uploadAkzeptieren(
                        eq(PRODUKT_VERSION_ID), eq(null), eq("trivy"), any()))
                .willReturn(new ScanUploadResponse(scanId, "/api/v1/scans/" + scanId));

        mockMvc.perform(post("/api/v1/scans")
                        .param("productVersionId", PRODUKT_VERSION_ID.toString())
                        .param("scanner", "trivy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bomFormat\":\"CycloneDX\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
