package com.ahs.cvm.api.kpi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.kpi.KpiService;
import com.ahs.cvm.application.kpi.KpiService.BurnDownPoint;
import com.ahs.cvm.application.kpi.KpiService.KpiResult;
import com.ahs.cvm.application.kpi.KpiService.SlaBucket;
import com.ahs.cvm.domain.enums.AhsSeverity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = KpiController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class KpiControllerWebTest {

    private static final UUID PV = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID ENV = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    @Autowired MockMvc mockMvc;
    @MockBean KpiService service;

    private KpiResult stubResult() {
        Map<AhsSeverity, Long> open = new EnumMap<>(AhsSeverity.class);
        for (AhsSeverity s : AhsSeverity.values()) {
            open.put(s, 0L);
        }
        open.put(AhsSeverity.HIGH, 3L);
        Map<AhsSeverity, Long> mttr = new EnumMap<>(AhsSeverity.class);
        Map<AhsSeverity, SlaBucket> sla = new EnumMap<>(AhsSeverity.class);
        for (AhsSeverity s : AhsSeverity.values()) {
            mttr.put(s, 0L);
            sla.put(s, new SlaBucket(0, 0));
        }
        return new KpiResult(open,
                List.of(new BurnDownPoint(LocalDate.of(2026, 4, 17), 5L)),
                mttr, sla, 0.25,
                Instant.parse("2026-04-18T10:00:00Z"));
    }

    @Test
    @DisplayName("GET /kpis: JSON mit offen-Bucket")
    void json() throws Exception {
        given(service.compute(any(), any(), any())).willReturn(stubResult());
        mockMvc.perform(get("/api/v1/kpis")
                        .param("productVersionId", PV.toString())
                        .param("environmentId", ENV.toString())
                        .param("window", "90d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openBySeverity.HIGH").value(3))
                .andExpect(jsonPath("$.automationRate").value(0.25));
    }

    @Test
    @DisplayName("GET /kpis/export: CSV mit section-Zeilen")
    void csv() throws Exception {
        given(service.compute(any(), any(), any())).willReturn(stubResult());
        mockMvc.perform(get("/api/v1/kpis/export")
                        .param("window", "90d"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("open,HIGH,3")));
    }

    @Test
    @DisplayName("GET /kpis: ungueltiges window-Format -> 400")
    void badWindow() throws Exception {
        mockMvc.perform(get("/api/v1/kpis").param("window", "xxx"))
                .andExpect(status().isBadRequest());
    }
}
