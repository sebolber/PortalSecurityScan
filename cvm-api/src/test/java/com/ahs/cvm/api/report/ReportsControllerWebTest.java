package com.ahs.cvm.api.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.report.GeneratedReportView;
import com.ahs.cvm.application.report.ReportGeneratorService;
import com.ahs.cvm.application.report.ReportNotFoundException;
import com.ahs.cvm.domain.enums.AhsSeverity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
        controllers = ReportsController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import({ReportsExceptionHandler.class, ReportsControllerWebTest.TestBeans.class})
class ReportsControllerWebTest {

    @Autowired MockMvc mockMvc;

    @MockBean ReportGeneratorService service;

    private static final UUID REPORT_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID PRODUCT_VERSION_ID =
            UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID ENVIRONMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final Instant ERZEUGT_AM = Instant.parse("2026-04-18T10:00:00Z");

    @org.springframework.boot.test.context.TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        Clock clock() {
            return Clock.fixed(ERZEUGT_AM, ZoneOffset.UTC);
        }
    }

    @Test
    @DisplayName("POST /reports/hardening: 201 Created liefert reportId und sha256")
    void erzeugen() throws Exception {
        given(service.generateHardeningReport(any())).willReturn(view());

        String body = """
                {
                  "productVersionId": "00000000-0000-0000-0000-0000000000aa",
                  "environmentId": "00000000-0000-0000-0000-0000000000bb",
                  "gesamteinstufung": "MEDIUM",
                  "freigeberKommentar": "ok",
                  "erzeugtVon": "a.admin@ahs.test"
                }
                """;
        mockMvc.perform(post("/api/v1/reports/hardening")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(REPORT_ID.toString()))
                .andExpect(jsonPath("$.gesamteinstufung").value("MEDIUM"))
                .andExpect(jsonPath("$.sha256").value("abc123"))
                .andExpect(header().string("Location",
                        "/api/v1/reports/" + REPORT_ID));
    }

    @Test
    @DisplayName("POST /reports/hardening: 400 bei leerem erzeugtVon")
    void pflichtfelder() throws Exception {
        String body = """
                {
                  "productVersionId": "00000000-0000-0000-0000-0000000000aa",
                  "environmentId": "00000000-0000-0000-0000-0000000000bb",
                  "gesamteinstufung": "MEDIUM",
                  "erzeugtVon": ""
                }
                """;
        mockMvc.perform(post("/api/v1/reports/hardening")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /reports/{id}: liefert application/pdf mit SHA-Header")
    void downloadPdf() throws Exception {
        given(service.findById(eq(REPORT_ID))).willReturn(view());

        mockMvc.perform(get("/api/v1/reports/" + REPORT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("X-Report-Sha256", "abc123"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"hardening-report-" + REPORT_ID + ".pdf\""));
    }

    @Test
    @DisplayName("GET /reports: pagenierte Liste ohne PDF-Bytes")
    void liste() throws Exception {
        given(service.list(eq(PRODUCT_VERSION_ID), eq(ENVIRONMENT_ID),
                eq(0), eq(20)))
                .willReturn(new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(view().withoutBytes()),
                        org.springframework.data.domain.PageRequest.of(0, 20),
                        1));
        mockMvc.perform(get("/api/v1/reports")
                        .param("productVersionId", PRODUCT_VERSION_ID.toString())
                        .param("environmentId", ENVIRONMENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].reportId").value(REPORT_ID.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @DisplayName("GET /reports/{id}: 404 bei unbekannter reportId")
    void downloadNotFound() throws Exception {
        UUID unbekannt = UUID.fromString("55555555-5555-5555-5555-555555555555");
        willThrow(new ReportNotFoundException(unbekannt))
                .given(service).findById(eq(unbekannt));

        mockMvc.perform(get("/api/v1/reports/" + unbekannt))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("report_not_found"))
                .andExpect(jsonPath("$.reportId").value(unbekannt.toString()));
    }

    private GeneratedReportView view() {
        return new GeneratedReportView(
                REPORT_ID,
                PRODUCT_VERSION_ID,
                ENVIRONMENT_ID,
                "HARDENING",
                "Hardening-Report PortalCore-Test 1.14.2-test (REF-TEST)",
                AhsSeverity.MEDIUM,
                "a.admin@ahs.test",
                ERZEUGT_AM,
                ERZEUGT_AM,
                "abc123",
                new byte[] {37, 80, 68, 70});
    }
}
