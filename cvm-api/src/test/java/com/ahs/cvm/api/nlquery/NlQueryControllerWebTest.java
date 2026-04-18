package com.ahs.cvm.api.nlquery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.ai.nlquery.NlFilter;
import com.ahs.cvm.ai.nlquery.NlQueryResult;
import com.ahs.cvm.ai.nlquery.NlQueryService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = NlQueryController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class NlQueryControllerWebTest {

    @Autowired MockMvc mockMvc;
    @MockBean NlQueryService service;

    @Test
    @DisplayName("POST /dashboard/query: 200 mit Filter und results")
    void ok() throws Exception {
        NlFilter filter = new NlFilter("PROD", null, List.of(), List.of(),
                null, null, null, NlFilter.SortBy.AGE_DESC);
        given(service.query(any(), any())).willReturn(
                new NlQueryResult(filter, "ok", List.of(), List.of()));
        mockMvc.perform(post("/api/v1/dashboard/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nlQuestion\":\"HIGH in PROD\",\"triggeredBy\":\"t@x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filter.environmentKey").value("PROD"))
                .andExpect(jsonPath("$.rejectedReasons").isEmpty());
    }

    @Test
    @DisplayName("POST /dashboard/query: 422 bei unbekanntem Feld")
    void unprocessable() throws Exception {
        given(service.query(any(), any())).willReturn(
                new NlQueryResult(null, "", List.of(),
                        List.of("Unbekanntes Filterfeld: deleteAll")));
        mockMvc.perform(post("/api/v1/dashboard/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nlQuestion\":\"drop all\",\"triggeredBy\":\"t@x\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.rejectedReasons[0]").value(
                        org.hamcrest.Matchers.containsString("deleteAll")));
    }

    @Test
    @DisplayName("POST /dashboard/query: 400 bei leerer Frage")
    void badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/dashboard/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nlQuestion\":\"\",\"triggeredBy\":\"t@x\"}"))
                .andExpect(status().isBadRequest());
    }
}
