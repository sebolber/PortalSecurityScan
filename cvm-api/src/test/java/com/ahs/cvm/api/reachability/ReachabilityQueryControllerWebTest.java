package com.ahs.cvm.api.reachability;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.ai.reachability.ReachabilityAgent;
import com.ahs.cvm.application.reachability.ReachabilityQueryService;
import com.ahs.cvm.application.reachability.ReachabilitySummaryView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AiSuggestionStatus;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = ReachabilityQueryController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class ReachabilityQueryControllerWebTest {

    @Autowired MockMvc mockMvc;
    @MockBean ReachabilityQueryService service;
    // Satisfies ReachabilityController constructor (same @ComponentScan).
    @MockBean ReachabilityAgent reachabilityAgent;

    @Test
    @DisplayName("GET /api/v1/reachability: liefert letzte Analysen")
    void liste() throws Exception {
        given(service.recent(50)).willReturn(List.of(
                new ReachabilitySummaryView(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        AiSuggestionStatus.PROPOSED,
                        AhsSeverity.HIGH,
                        "Funktion erreichbar ueber REST-Handler",
                        new BigDecimal("0.82"),
                        Instant.parse("2026-04-18T10:00:00Z"))));

        mockMvc.perform(get("/api/v1/reachability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].severity").value("HIGH"))
                .andExpect(jsonPath("$[0].rationale")
                        .value("Funktion erreichbar ueber REST-Handler"));
    }
}
