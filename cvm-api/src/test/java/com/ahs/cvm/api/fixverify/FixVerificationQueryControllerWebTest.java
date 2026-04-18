package com.ahs.cvm.api.fixverify;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.ai.fixverify.FixVerificationService;
import com.ahs.cvm.application.fixverification.FixVerificationQueryService;
import com.ahs.cvm.application.fixverification.FixVerificationSummaryView;
import com.ahs.cvm.domain.enums.FixEvidenceType;
import com.ahs.cvm.domain.enums.FixVerificationGrade;
import com.ahs.cvm.domain.enums.MitigationStatus;
import com.ahs.cvm.domain.enums.MitigationStrategy;
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
        controllers = FixVerificationQueryController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class FixVerificationQueryControllerWebTest {

    @Autowired MockMvc mockMvc;
    @MockBean FixVerificationQueryService service;
    // Satisfies FixVerificationController constructor (same @ComponentScan).
    @MockBean FixVerificationService fixVerificationService;

    @Test
    @DisplayName("GET /api/v1/fix-verification: liefert letzte Eintraege")
    void listeDefault() throws Exception {
        given(service.recent(50)).willReturn(List.of(view(FixVerificationGrade.A)));

        mockMvc.perform(get("/api/v1/fix-verification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].verificationGrade").value("A"));
    }

    @Test
    @DisplayName("GET /api/v1/fix-verification?grade=B: filtert ueber Service")
    void filterByGrade() throws Exception {
        given(service.byGrade(FixVerificationGrade.B, 20))
                .willReturn(List.of(view(FixVerificationGrade.B)));

        mockMvc.perform(get("/api/v1/fix-verification")
                        .param("grade", "B")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].verificationGrade").value("B"));
    }

    private static FixVerificationSummaryView view(FixVerificationGrade grade) {
        return new FixVerificationSummaryView(
                UUID.randomUUID(),
                UUID.randomUUID(),
                MitigationStatus.IMPLEMENTED,
                MitigationStrategy.UPGRADE,
                "2.3.4",
                "dev-team@ahs.test",
                Instant.parse("2026-04-01T08:00:00Z"),
                Instant.parse("2026-04-10T08:00:00Z"),
                grade,
                FixEvidenceType.FIX_COMMIT_MATCH,
                Instant.parse("2026-04-11T12:00:00Z"),
                Instant.parse("2026-03-29T12:00:00Z"));
    }
}
