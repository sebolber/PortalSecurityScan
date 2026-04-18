package com.ahs.cvm.api.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.ai.copilot.CopilotService;
import com.ahs.cvm.ai.copilot.CopilotSuggestion;
import com.ahs.cvm.ai.copilot.CopilotUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(
        controllers = CopilotController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(CopilotExceptionHandler.class)
class CopilotControllerWebTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final UUID ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired MockMvc mockMvc;
    @MockBean CopilotService copilotService;

    @Test
    @DisplayName("Copilot-Controller: NDJSON liefert text- und sources-Zeile")
    void streamHappyPath() throws Exception {
        given(copilotService.suggest(any())).willReturn(new CopilotSuggestion(
                ID, CopilotUseCase.REFINE_RATIONALE,
                "Praezise Begruendung.",
                List.of(new CopilotSuggestion.SourceRef("DOCUMENT", "ref-1", "..."))));

        String body = """
                {
                  "useCase":"REFINE_RATIONALE",
                  "userInstruction":"verfeinern",
                  "triggeredBy":"t.tester@ahs.test"
                }""";
        MvcResult start = mockMvc.perform(post("/api/v1/assessments/" + ID + "/copilot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult end = mockMvc.perform(asyncDispatch(start))
                .andExpect(status().isOk())
                .andReturn();

        String content = end.getResponse().getContentAsString();
        String[] lines = content.split("\\R");
        assertThat(lines).hasSize(2);
        JsonNode line1 = MAPPER.readTree(lines[0]);
        JsonNode line2 = MAPPER.readTree(lines[1]);
        assertThat(line1.path("type").asText()).isEqualTo("text");
        assertThat(line1.path("content").asText()).isEqualTo("Praezise Begruendung.");
        assertThat(line1.path("useCase").asText()).isEqualTo("REFINE_RATIONALE");
        assertThat(line2.path("type").asText()).isEqualTo("sources");
        assertThat(line2.path("items").isArray()).isTrue();
        assertThat(line2.path("items").size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Copilot-Controller: 400 bei leerer Instruction")
    void leereInstruction() throws Exception {
        String body = """
                {
                  "useCase":"REFINE_RATIONALE",
                  "userInstruction":"",
                  "triggeredBy":"t@x"
                }""";
        mockMvc.perform(post("/api/v1/assessments/" + ID + "/copilot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
