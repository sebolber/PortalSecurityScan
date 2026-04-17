package com.ahs.cvm.api.assessment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.assessment.AssessmentFourEyesViolationException;
import com.ahs.cvm.application.assessment.AssessmentNotFoundException;
import com.ahs.cvm.application.assessment.AssessmentWriteService;
import com.ahs.cvm.application.assessment.FindingQueueView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import java.time.Instant;
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
        controllers = AssessmentsController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(AssessmentsExceptionHandler.class)
class AssessmentsControllerWebTest {

    @Autowired MockMvc mockMvc;

    @MockBean AssessmentWriteService writeService;

    private static final UUID ASSESSMENT_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    @DisplayName("POST /assessments: 201 Created mit Status PROPOSED")
    void vorschlagAnlegen() throws Exception {
        given(writeService.manualProposeView(any()))
                .willReturn(view(AhsSeverity.HIGH, AssessmentStatus.PROPOSED));

        String body = """
                {
                  "findingId": "11111111-1111-1111-1111-111111111111",
                  "ahsSeverity": "HIGH",
                  "rationale": "Produkt nutzt verwundbares Modul",
                  "decidedBy": "t.tester@ahs.test"
                }
                """;

        mockMvc.perform(post("/api/v1/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROPOSED"))
                .andExpect(jsonPath("$.severity").value("HIGH"));
    }

    @Test
    @DisplayName("POST /assessments: 400 bei fehlendem Pflichtfeld rationale")
    void validierungFehlt() throws Exception {
        String body = """
                {
                  "findingId": "11111111-1111-1111-1111-111111111111",
                  "ahsSeverity": "HIGH",
                  "decidedBy": "t.tester@ahs.test"
                }
                """;

        mockMvc.perform(post("/api/v1/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /assessments/{id}/approve: 409 bei Vier-Augen-Verstoss")
    void approveVierAugenKonflikt() throws Exception {
        willThrow(new AssessmentFourEyesViolationException(
                        "Vier-Augen-Prinzip verletzt: Downgrade auf NOT_APPLICABLE"))
                .given(writeService)
                .approveView(eq(ASSESSMENT_ID), eq("t.tester@ahs.test"), any());

        mockMvc.perform(post("/api/v1/assessments/{id}/approve", ASSESSMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approverId\":\"t.tester@ahs.test\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("assessment_four_eyes_violation"));
    }

    @Test
    @DisplayName("POST /assessments/{id}/approve: 200 OK mit Status APPROVED")
    void approveOK() throws Exception {
        given(writeService.approveView(eq(ASSESSMENT_ID), eq("a.admin@ahs.test"), any()))
                .willReturn(view(AhsSeverity.HIGH, AssessmentStatus.APPROVED));

        mockMvc.perform(post("/api/v1/assessments/{id}/approve", ASSESSMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approverId\":\"a.admin@ahs.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /assessments/{id}/reject: 404 bei unbekannter Assessment-ID")
    void rejectNichtGefunden() throws Exception {
        willThrow(new AssessmentNotFoundException(ASSESSMENT_ID))
                .given(writeService).rejectView(eq(ASSESSMENT_ID), any(), any());

        mockMvc.perform(post("/api/v1/assessments/{id}/reject", ASSESSMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approverId\":\"a.admin@ahs.test\","
                                + "\"comment\":\"False-Positive\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("assessment_not_found"));
    }

    private FindingQueueView view(AhsSeverity severity, AssessmentStatus status) {
        return new FindingQueueView(
                ASSESSMENT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CVE-2017-18640",
                UUID.randomUUID(),
                UUID.randomUUID(),
                severity,
                status,
                ProposalSource.HUMAN,
                "Begruendung",
                "t.tester@ahs.test",
                Instant.now(),
                1);
    }
}
