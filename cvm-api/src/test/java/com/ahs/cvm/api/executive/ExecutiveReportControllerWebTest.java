package com.ahs.cvm.api.executive;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.ai.executive.ExecutiveReportService;
import com.ahs.cvm.ai.executive.ExecutiveReportService.Audience;
import com.ahs.cvm.ai.executive.ExecutiveReportService.ExecutiveReportResult;
import com.ahs.cvm.ai.executive.ExecutiveSummary;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = ExecutiveReportController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class ExecutiveReportControllerWebTest {

    private static final UUID PV = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID ENV = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    @Autowired MockMvc mockMvc;
    @MockBean ExecutiveReportService service;

    @Test
    @DisplayName("GET /reports/executive: liefert PDF + X-Executive-Ampel-Header")
    void board() throws Exception {
        given(service.generate(eq(PV), eq(ENV), eq(Audience.BOARD), any()))
                .willReturn(new ExecutiveReportResult(
                        new ExecutiveSummary("H", "YELLOW", List.of("A")),
                        new byte[]{0x25, 0x50, 0x44, 0x46}));

        mockMvc.perform(get("/api/v1/reports/executive")
                        .param("productVersionId", PV.toString())
                        .param("environmentId", ENV.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("X-Executive-Ampel", "YELLOW"));
    }

    @Test
    @DisplayName("GET /reports/executive?audience=audit: ruft Audit-Variante auf")
    void audit() throws Exception {
        given(service.generate(any(), any(), eq(Audience.AUDIT), any()))
                .willReturn(new ExecutiveReportResult(
                        new ExecutiveSummary("H", "RED", List.of()),
                        new byte[]{0x25, 0x50, 0x44, 0x46}));

        mockMvc.perform(get("/api/v1/reports/executive")
                        .param("productVersionId", PV.toString())
                        .param("environmentId", ENV.toString())
                        .param("audience", "audit"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Executive-Ampel", "RED"));
    }
}
