package com.ahs.cvm.api.rag;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.ai.rag.IndexingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = RagAdminController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class RagAdminControllerWebTest {

    @Autowired MockMvc mockMvc;

    @MockBean IndexingService indexingService;

    @Test
    @DisplayName("RAG-Admin: POST /reindex liefert die Anzahl indexierter Chunks")
    void reindex() throws Exception {
        given(indexingService.indexAll()).willReturn(42);

        mockMvc.perform(post("/api/v1/admin/rag/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunks").value(42));
    }
}
