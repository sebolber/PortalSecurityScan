package com.ahs.cvm.api.pipeline;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.pipeline.PipelineGateService;
import com.ahs.cvm.application.pipeline.PipelineGateService.GateDecision;
import com.ahs.cvm.application.pipeline.PipelineGateService.GateResult;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = PipelineGateController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(PipelineGateExceptionHandler.class)
class PipelineGateControllerWebTest {

    private static final UUID PV = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @Autowired MockMvc mockMvc;
    @MockBean PipelineGateService service;

    private GateResult stub(GateDecision d, int crit, int high) {
        return new GateResult(d, crit, high,
                Instant.parse("2026-04-18T10:00:00Z"), List.of());
    }

    @Test
    @DisplayName("POST /pipeline/gate: PASS mit Decision-Header")
    void pass() throws Exception {
        given(service.evaluate(any())).willReturn(stub(GateDecision.PASS, 0, 0));
        MockMultipartFile sbom = new MockMultipartFile(
                "sbom", "sbom.json", "application/json",
                "{\"bomFormat\":\"CycloneDX\"}".getBytes());
        mockMvc.perform(multipart("/api/v1/pipeline/gate")
                        .file(sbom)
                        .param("productVersionId", PV.toString())
                        .param("branchRef", "main")
                        .param("mergeRequestId", "MR-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Gate-Decision", "PASS"))
                .andExpect(jsonPath("$.gate").value("PASS"));
    }

    @Test
    @DisplayName("POST /pipeline/gate: FAIL enthaelt Zaehler")
    void fail() throws Exception {
        given(service.evaluate(any())).willReturn(stub(GateDecision.FAIL, 2, 0));
        MockMultipartFile sbom = new MockMultipartFile(
                "sbom", "sbom.json", "application/json", "{}".getBytes());
        mockMvc.perform(multipart("/api/v1/pipeline/gate")
                        .file(sbom)
                        .param("productVersionId", PV.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gate").value("FAIL"))
                .andExpect(jsonPath("$.newCritical").value(2));
    }

    @Test
    @DisplayName("POST /pipeline/gate: leere SBOM -> 400")
    void leer() throws Exception {
        MockMultipartFile sbom = new MockMultipartFile(
                "sbom", "sbom.json", "application/json", new byte[0]);
        mockMvc.perform(multipart("/api/v1/pipeline/gate")
                        .file(sbom)
                        .param("productVersionId", PV.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("gate_bad_request"));
    }
}
