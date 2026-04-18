package com.ahs.cvm.api.profileassistant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.ai.profileassistant.ProfileAssistantService;
import com.ahs.cvm.ai.profileassistant.ProfileAssistantService.FinalizeResult;
import com.ahs.cvm.ai.profileassistant.ProfileAssistantService.ReplyResult;
import com.ahs.cvm.ai.profileassistant.ProfileAssistantService.StartResult;
import java.time.Instant;
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

@WebMvcTest(
        controllers = ProfileAssistantController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(ProfileAssistantExceptionHandler.class)
class ProfileAssistantControllerWebTest {

    private static final UUID ENV = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final UUID SESSION = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired MockMvc mockMvc;
    @MockBean ProfileAssistantService service;

    @Test
    @DisplayName("Start: 200 mit sessionId und question")
    void start() throws Exception {
        given(service.start(eq(ENV), eq("u@x"))).willReturn(
                new StartResult(SESSION, "Erste Frage?", Instant.now()));
        mockMvc.perform(post("/api/v1/environments/" + ENV + "/profile/assist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startedBy\":\"u@x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(SESSION.toString()))
                .andExpect(jsonPath("$.question").value("Erste Frage?"));
    }

    @Test
    @DisplayName("Reply: 200 mit naechster Frage")
    void reply() throws Exception {
        given(service.reply(eq(SESSION), eq("architecture.x"), eq("ja")))
                .willReturn(new ReplyResult(SESSION, "Naechste Frage?", false));
        mockMvc.perform(post("/api/v1/environments/" + ENV + "/profile/assist/"
                        + SESSION + "/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldPath\":\"architecture.x\",\"answer\":\"ja\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextQuestion").value("Naechste Frage?"));
    }

    @Test
    @DisplayName("Finalize: 200 mit Draft-Id")
    void finalize_() throws Exception {
        UUID draftId = UUID.randomUUID();
        given(service.finalize(eq(SESSION), eq("a.admin@ahs.test")))
                .willReturn(new FinalizeResult(SESSION, draftId, 3));
        mockMvc.perform(post("/api/v1/environments/" + ENV + "/profile/assist/"
                        + SESSION + "/finalize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proposedBy\":\"a.admin@ahs.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draftProfileId").value(draftId.toString()))
                .andExpect(jsonPath("$.versionNumber").value(3));
    }

    @Test
    @DisplayName("Start: 404 bei unbekannter Umgebung")
    void envNotFound() throws Exception {
        willThrow(new IllegalArgumentException("Umgebung nicht gefunden: " + ENV))
                .given(service).start(any(), any());
        mockMvc.perform(post("/api/v1/environments/" + ENV + "/profile/assist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startedBy\":\"u@x\"}"))
                .andExpect(status().isNotFound());
    }
}
