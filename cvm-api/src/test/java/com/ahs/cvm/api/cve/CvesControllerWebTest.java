package com.ahs.cvm.api.cve;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.cve.CveQueryService;
import com.ahs.cvm.application.cve.CveView;
import com.ahs.cvm.domain.enums.AhsSeverity;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = CvesController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class CvesControllerWebTest {

    @Autowired MockMvc mockMvc;
    @MockBean CveQueryService service;

    private CveView sample(String cveKey, double cvss) {
        return new CveView(
                UUID.randomUUID(),
                cveKey,
                "Beispiel-Summary " + cveKey,
                BigDecimal.valueOf(cvss),
                "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H",
                false,
                new BigDecimal("0.1234"),
                new BigDecimal("0.9500"),
                List.of("CWE-79"),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-03-01T00:00:00Z"),
                "NVD");
    }

    @Test
    @DisplayName("GET /api/v1/cves: Default-Paging liefert Items + Gesamtzahl")
    void list() throws Exception {
        given(service.findPage(any(), any(), anyBoolean(), anyInt(), anyInt()))
                .willReturn(new PageImpl<>(
                        List.of(sample("CVE-2026-0001", 9.5),
                                sample("CVE-2026-0002", 7.1)),
                        PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/api/v1/cves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].cveId").value("CVE-2026-0001"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/cves?severity=CRITICAL&kev=true: Parameter erreichen Service")
    void filter() throws Exception {
        given(service.findPage(eq("spring"), eq(AhsSeverity.CRITICAL), eq(true),
                eq(1), eq(10)))
                .willReturn(new PageImpl<>(List.of(sample("CVE-2017-5638", 10.0)),
                        PageRequest.of(1, 10), 11));

        mockMvc.perform(get("/api/v1/cves")
                        .param("q", "spring")
                        .param("severity", "CRITICAL")
                        .param("kev", "true")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(11));
    }
}
