package com.ahs.cvm.api.finding;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.assessment.AssessmentQueueService;
import com.ahs.cvm.application.assessment.FindingQueueView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
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
        controllers = FindingsController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class FindingsControllerWebTest {

    @Autowired MockMvc mockMvc;

    @MockBean AssessmentQueueService queueService;

    @Test
    @DisplayName("GET /findings liefert offene Vorschlaege als JSON-Array")
    void queueListe() throws Exception {
        given(queueService.findeOffene(any())).willReturn(List.of(view()));

        mockMvc.perform(get("/api/v1/findings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PROPOSED"))
                .andExpect(jsonPath("$[0].severity").value("MEDIUM"))
                .andExpect(jsonPath("$[0].cveKey").value("CVE-2017-18640"));
    }

    @Test
    @DisplayName("GET /findings?status=APPROVED liefert leere Liste")
    void unzulaessigerStatus() throws Exception {
        given(queueService.findeOffene(any())).willReturn(List.of());

        mockMvc.perform(get("/api/v1/findings").param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private FindingQueueView view() {
        return new FindingQueueView(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CVE-2017-18640",
                UUID.randomUUID(),
                UUID.randomUUID(),
                AhsSeverity.MEDIUM,
                AssessmentStatus.PROPOSED,
                ProposalSource.RULE,
                "regel-treffer",
                "system:rule",
                Instant.now(),
                1);
    }
}
