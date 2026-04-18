package com.ahs.cvm.api.alert;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.alert.AlertBannerService;
import com.ahs.cvm.application.alert.AlertBannerService.BannerStatus;
import com.ahs.cvm.application.alert.AlertConfig;
import com.ahs.cvm.application.alert.AlertEvaluator;
import com.ahs.cvm.application.alert.AlertEvaluator.AlertOutcome;
import com.ahs.cvm.application.alert.AlertRuleService;
import com.ahs.cvm.application.alert.AlertRuleView;
import com.ahs.cvm.domain.enums.AlertSeverity;
import com.ahs.cvm.domain.enums.AlertTriggerArt;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = AlertsController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(AlertsControllerWebTest.TestBeans.class)
class AlertsControllerWebTest {

    @Autowired MockMvc mockMvc;

    @MockBean AlertRuleService ruleService;
    @MockBean AlertEvaluator evaluator;
    @MockBean AlertBannerService bannerService;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-04-18T10:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        @Primary
        AlertConfig alertConfig() {
            return new AlertConfig("dry-run", 120, 360, "from@ahs.test");
        }
    }

    @Test
    @DisplayName("GET /alerts/rules liefert die Liste")
    void liste() throws Exception {
        given(ruleService.findeAlle()).willReturn(List.of(view()));
        mockMvc.perform(get("/api/v1/alerts/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("KEV-Hit"));
    }

    @Test
    @DisplayName("POST /alerts/test ruft den Evaluator und liefert Outcome")
    void testTrigger() throws Exception {
        given(evaluator.evaluate(any())).willReturn(new AlertOutcome(2, 1));
        String json = """
            {
              "triggerArt": "KEV_HIT",
              "triggerKey": "CVE-X|prod",
              "severity": "CRITICAL",
              "summary": "synth",
              "attributes": { "cveKey": "CVE-X" }
            }
            """;
        mockMvc.perform(post("/api/v1/alerts/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gefeuert").value(2))
                .andExpect(jsonPath("$.unterdrueckt").value(1))
                .andExpect(jsonPath("$.dryRunGlobal").value(true));
    }

    @Test
    @DisplayName("GET /alerts/banner liefert Banner-Status")
    void banner() throws Exception {
        given(bannerService.aktuellerStatus()).willReturn(new BannerStatus(true, 3, 360));
        mockMvc.perform(get("/api/v1/alerts/banner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visible").value(true))
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.t2Minutes").value(360));
    }

    private AlertRuleView view() {
        return new AlertRuleView(
                UUID.randomUUID(),
                "KEV-Hit",
                null,
                AlertTriggerArt.KEV_HIT,
                AlertSeverity.CRITICAL,
                60,
                "[CVM]",
                "alert-kev-hit",
                List.of("alerts@ahs.test"),
                true,
                Instant.now(),
                null);
    }
}
