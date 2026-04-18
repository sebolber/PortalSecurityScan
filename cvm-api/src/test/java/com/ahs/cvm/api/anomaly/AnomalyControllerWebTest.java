package com.ahs.cvm.api.anomaly;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.ai.anomaly.AnomalyQueryService;
import com.ahs.cvm.ai.anomaly.AnomalyQueryService.AnomalyView;
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
        controllers = AnomalyController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AnomalyControllerWebTest {

    @Autowired MockMvc mockMvc;
    @MockBean AnomalyQueryService service;

    @Test
    @DisplayName("GET /anomalies: liefert Liste")
    void liste() throws Exception {
        given(service.liste(any())).willReturn(List.of(
                new AnomalyView(UUID.randomUUID(), UUID.randomUUID(),
                        "KEV_NOT_APPLICABLE", "WARNING", "reason",
                        Instant.parse("2026-04-18T10:00:00Z"))));

        mockMvc.perform(get("/api/v1/anomalies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pattern").value("KEV_NOT_APPLICABLE"))
                .andExpect(jsonPath("$[0].severity").value("WARNING"));
    }

    @Test
    @DisplayName("GET /anomalies/count: liefert Count")
    void count() throws Exception {
        given(service.count24h(any())).willReturn(7L);
        mockMvc.perform(get("/api/v1/anomalies/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(7));
    }
}
