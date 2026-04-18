package com.ahs.cvm.api.fixverify;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.ai.fixverify.FixVerificationResult;
import com.ahs.cvm.ai.fixverify.FixVerificationService;
import com.ahs.cvm.domain.enums.FixEvidenceType;
import com.ahs.cvm.domain.enums.FixVerificationGrade;
import java.math.BigDecimal;
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
        controllers = FixVerificationController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(FixVerificationExceptionHandler.class)
class FixVerificationControllerWebTest {

    private static final UUID MIT = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final String BODY = """
            {
              "repoUrl":"https://github.com/foo/bar",
              "fromVersion":"v1.2.2",
              "toVersion":"v1.2.3",
              "vulnerableSymbol":"Parser.parse(String)",
              "triggeredBy":"t@x"
            }""";

    @Autowired MockMvc mockMvc;
    @MockBean FixVerificationService service;
    // Satisfies FixVerificationQueryController constructor (same @ComponentScan).
    @MockBean com.ahs.cvm.application.fixverification.FixVerificationQueryService fixVerificationQueryService;

    @Test
    @DisplayName("POST /verify-fix: 200 mit Grade + Evidenz")
    void verify() throws Exception {
        given(service.verify(any())).willReturn(new FixVerificationResult(
                MIT, UUID.randomUUID(),
                FixVerificationGrade.A, FixEvidenceType.EXPLICIT_CVE_MENTION,
                BigDecimal.valueOf(0.9),
                List.of(new FixVerificationResult.CommitEvidence("abc", "fix", "url")),
                List.of("ok"),
                Instant.parse("2026-04-18T10:00:00Z"),
                true, null));

        mockMvc.perform(post("/api/v1/mitigations/" + MIT + "/verify-fix")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").value("A"))
                .andExpect(jsonPath("$.evidenceType").value("EXPLICIT_CVE_MENTION"))
                .andExpect(jsonPath("$.addressedBy[0].commit").value("abc"));
    }

    @Test
    @DisplayName("GET /verification: liefert aktuellen Plan-Stand")
    void load() throws Exception {
        given(service.load(MIT)).willReturn(new FixVerificationResult(
                MIT, null, FixVerificationGrade.UNKNOWN, FixEvidenceType.NONE,
                BigDecimal.ZERO, List.of(), List.of(), null, false, "nicht verifiziert"));
        mockMvc.perform(get("/api/v1/mitigations/" + MIT + "/verification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").value("UNKNOWN"))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @DisplayName("POST /verify-fix: 404 bei unbekannter Mitigation")
    void notFound() throws Exception {
        willThrow(new IllegalArgumentException("Mitigation nicht gefunden: " + MIT))
                .given(service).verify(any());
        mockMvc.perform(post("/api/v1/mitigations/" + MIT + "/verify-fix")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("mitigation_not_found"));
    }

    @Test
    @DisplayName("POST /verify-fix: 400 bei leeren Versionsangaben")
    void badRequest() throws Exception {
        String invalid = """
                {"repoUrl":"","fromVersion":"","triggeredBy":""}""";
        mockMvc.perform(post("/api/v1/mitigations/" + MIT + "/verify-fix")
                        .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
    }
}
