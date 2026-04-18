package com.ahs.cvm.api.summary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.ai.summary.ScanDelta;
import com.ahs.cvm.ai.summary.ScanDeltaSummary;
import com.ahs.cvm.ai.summary.ScanDeltaSummaryService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = DeltaSummaryController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(DeltaSummaryExceptionHandler.class)
class DeltaSummaryControllerWebTest {

    private static final UUID SCAN = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID PREV = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Autowired MockMvc mockMvc;
    @MockBean ScanDeltaSummaryService service;

    private ScanDeltaSummary fakeSummary() {
        return new ScanDeltaSummary(
                SCAN, PREV,
                "Kurztext.",
                "Langtext mit Lenkungsausschuss-Inhalten.",
                new ScanDelta(List.of("CVE-NEU"), List.of(),
                        List.of(new ScanDelta.SeverityShift("CVE-A", "HIGH", "CRITICAL")),
                        List.of("CVE-K")),
                true);
    }

    @Test
    @DisplayName("DeltaSummary: audience=short liefert shortText im summary-Feld")
    void shortAudience() throws Exception {
        given(service.summarize(SCAN)).willReturn(fakeSummary());

        mockMvc.perform(get("/api/v1/scans/" + SCAN + "/delta-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Kurztext."))
                .andExpect(jsonPath("$.llmAufgerufen").value(true))
                .andExpect(jsonPath("$.neueCves[0]").value("CVE-NEU"))
                .andExpect(jsonPath("$.severityShifts[0].cveKey").value("CVE-A"));
    }

    @Test
    @DisplayName("DeltaSummary: audience=long liefert longText")
    void longAudience() throws Exception {
        given(service.summarize(SCAN)).willReturn(fakeSummary());

        mockMvc.perform(get("/api/v1/scans/" + SCAN + "/delta-summary?audience=long"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Langtext mit Lenkungsausschuss-Inhalten."));
    }

    @Test
    @DisplayName("DeltaSummary: 404 bei unbekanntem Scan")
    void unbekannterScan() throws Exception {
        UUID id = UUID.randomUUID();
        willThrow(new IllegalArgumentException("Scan nicht gefunden: " + id))
                .given(service).summarize(any());

        mockMvc.perform(get("/api/v1/scans/" + id + "/delta-summary"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("scan_not_found"));
    }
}
