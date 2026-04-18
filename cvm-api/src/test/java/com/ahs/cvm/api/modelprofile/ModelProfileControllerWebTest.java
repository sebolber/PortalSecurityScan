package com.ahs.cvm.api.modelprofile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.modelprofile.ModelProfileQueryService;
import com.ahs.cvm.application.modelprofile.ModelProfileService;
import com.ahs.cvm.application.modelprofile.ModelProfileService.ModelProfileChangeView;
import com.ahs.cvm.application.modelprofile.ModelProfileService.VierAugenViolationException;
import java.time.Instant;
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

@WebMvcTest(
        controllers = ModelProfileController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(ModelProfileExceptionHandler.class)
class ModelProfileControllerWebTest {

    private static final UUID ENV = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID NEW_PROFILE = UUID.fromString(
            "22222222-2222-2222-2222-222222222222");

    @Autowired MockMvc mockMvc;
    @MockBean ModelProfileService service;
    // Seit Iteration 25 lebt ein zweiter Controller im gleichen Package;
    // sein Read-Service muss gemockt werden, damit der Kontext aufgeht.
    @MockBean ModelProfileQueryService queryService;

    private ModelProfileChangeView view() {
        return new ModelProfileChangeView(
                UUID.randomUUID(), ENV, null, NEW_PROFILE,
                "a.admin@ahs.test", "j.meyer@ahs.test",
                "Budget-Anpassung", Instant.parse("2026-04-18T10:00:00Z"));
    }

    @Test
    @DisplayName("POST /switch: 201 mit Change-View")
    void ok() throws Exception {
        given(service.switchProfile(any())).willReturn(view());
        mockMvc.perform(post("/api/v1/environments/" + ENV + "/model-profile/switch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newProfileId":"%s",
                                 "changedBy":"a.admin@ahs.test",
                                 "fourEyesConfirmer":"j.meyer@ahs.test",
                                 "reason":"Budget-Anpassung"}
                                """.formatted(NEW_PROFILE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newProfileId").value(NEW_PROFILE.toString()));
    }

    @Test
    @DisplayName("POST /switch: Vier-Augen-Verstoss -> 409")
    void vierAugen() throws Exception {
        willThrow(new VierAugenViolationException(
                "Vier-Augen-Verstoss: changedBy == fourEyesConfirmer (x)"))
                .given(service).switchProfile(any());
        mockMvc.perform(post("/api/v1/environments/" + ENV + "/model-profile/switch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newProfileId":"%s",
                                 "changedBy":"x","fourEyesConfirmer":"x"}
                                """.formatted(NEW_PROFILE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("vier_augen_violation"));
    }

    @Test
    @DisplayName("POST /switch: unbekannte Umgebung -> 404")
    void envFehlt() throws Exception {
        willThrow(new IllegalArgumentException("Environment nicht gefunden: " + ENV))
                .given(service).switchProfile(any());
        mockMvc.perform(post("/api/v1/environments/" + ENV + "/model-profile/switch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newProfileId":"%s",
                                 "changedBy":"a","fourEyesConfirmer":"b"}
                                """.formatted(NEW_PROFILE)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /switch: leerer changedBy -> 400")
    void validation() throws Exception {
        mockMvc.perform(post("/api/v1/environments/" + ENV + "/model-profile/switch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newProfileId":"%s",
                                 "changedBy":"","fourEyesConfirmer":"b"}
                                """.formatted(NEW_PROFILE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /history: Liste")
    void history() throws Exception {
        given(service.historieFuerEnvironment(ENV)).willReturn(List.of(view()));
        mockMvc.perform(get("/api/v1/environments/" + ENV + "/model-profile/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].newProfileId").value(NEW_PROFILE.toString()));
    }
}
