package com.ahs.cvm.api.modelprofile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.modelprofile.ModelProfileQueryService;
import com.ahs.cvm.application.modelprofile.ModelProfileService;
import com.ahs.cvm.application.modelprofile.ModelProfileService.ProfileKeyConflictException;
import com.ahs.cvm.application.modelprofile.ModelProfileService.VierAugenViolationException;
import com.ahs.cvm.application.modelprofile.ModelProfileView;
import java.math.BigDecimal;
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
        controllers = LlmModelProfilesController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(ModelProfileExceptionHandler.class)
class LlmModelProfilesControllerWebTest {

    @Autowired MockMvc mockMvc;
    @MockBean ModelProfileQueryService queryService;
    @MockBean ModelProfileService profileService;

    @Test
    @DisplayName("GET /api/v1/llm-model-profiles: Liste mit Profil-Keys")
    void list() throws Exception {
        given(queryService.listAll()).willReturn(List.of(
                new ModelProfileView(UUID.randomUUID(),
                        "CLAUDE_CLOUD_DEFAULT", "CLAUDE_CLOUD",
                        "claude-sonnet-4-6", "sonnet-4.6",
                        new BigDecimal("100.00"), true),
                new ModelProfileView(UUID.randomUUID(),
                        "OLLAMA_ONPREM_FALLBACK", "OLLAMA_ONPREM",
                        "llama3:8b", null,
                        BigDecimal.ZERO, true)));

        mockMvc.perform(get("/api/v1/llm-model-profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].profileKey").value("CLAUDE_CLOUD_DEFAULT"))
                .andExpect(jsonPath("$[0].provider").value("CLAUDE_CLOUD"))
                .andExpect(jsonPath("$[1].profileKey").value("OLLAMA_ONPREM_FALLBACK"));
    }

    @Test
    @DisplayName("GET: leere Liste fuehrt zu 200 mit []")
    void emptyList() throws Exception {
        given(queryService.listAll()).willReturn(List.of());
        mockMvc.perform(get("/api/v1/llm-model-profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("POST /llm-model-profiles: 201 Created, Location-Header")
    void anlegenHappyPath() throws Exception {
        UUID id = UUID.randomUUID();
        given(profileService.createProfile(any()))
                .willReturn(new ModelProfileView(id,
                        "CLAUDE_SANDBOX", "CLAUDE_CLOUD",
                        "claude-sonnet-4-6", null,
                        new BigDecimal("50.00"), false));

        mockMvc.perform(post("/api/v1/llm-model-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileKey": "CLAUDE_SANDBOX",
                                  "provider": "CLAUDE_CLOUD",
                                  "modelId": "claude-sonnet-4-6",
                                  "costBudgetEurMonthly": 50.00,
                                  "approvedForGkvData": false,
                                  "approvedBy": "a.admin@ahs.test"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/llm-model-profiles/" + id))
                .andExpect(jsonPath("$.profileKey").value("CLAUDE_SANDBOX"));
    }

    @Test
    @DisplayName("POST: GKV-Profil ohne Zweitfreigeber -> 400 (Validierungsfehler)")
    void gkvOhneZweitfreigeber() throws Exception {
        willThrow(new IllegalArgumentException(
                "fourEyesConfirmer (GKV-Freigabe erfordert Vier-Augen) darf nicht leer sein."))
                .given(profileService).createProfile(any());

        mockMvc.perform(post("/api/v1/llm-model-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileKey": "CLAUDE_GKV",
                                  "provider": "CLAUDE_CLOUD",
                                  "modelId": "claude-sonnet-4-6",
                                  "costBudgetEurMonthly": 100.00,
                                  "approvedForGkvData": true,
                                  "approvedBy": "a.admin@ahs.test"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("model_profile_bad_request"));
    }

    @Test
    @DisplayName("POST: Vier-Augen-Verstoss (gleicher User) -> 409")
    void vierAugenVerstoss() throws Exception {
        willThrow(new VierAugenViolationException(
                "Vier-Augen-Verstoss: approvedBy == fourEyesConfirmer (a.admin@ahs.test)"))
                .given(profileService).createProfile(any());

        mockMvc.perform(post("/api/v1/llm-model-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileKey": "CLAUDE_GKV",
                                  "provider": "CLAUDE_CLOUD",
                                  "modelId": "claude-sonnet-4-6",
                                  "costBudgetEurMonthly": 100.00,
                                  "approvedForGkvData": true,
                                  "approvedBy": "a.admin@ahs.test",
                                  "fourEyesConfirmer": "a.admin@ahs.test"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("vier_augen_violation"));
    }

    @Test
    @DisplayName("POST: doppelter profileKey -> 409")
    void keyKonflikt() throws Exception {
        willThrow(new ProfileKeyConflictException("CLAUDE_SANDBOX"))
                .given(profileService).createProfile(any());

        mockMvc.perform(post("/api/v1/llm-model-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileKey": "CLAUDE_SANDBOX",
                                  "provider": "CLAUDE_CLOUD",
                                  "modelId": "claude-sonnet-4-6",
                                  "costBudgetEurMonthly": 10.00,
                                  "approvedForGkvData": false,
                                  "approvedBy": "a.admin@ahs.test"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("profile_key_conflict"))
                .andExpect(jsonPath("$.profileKey").value("CLAUDE_SANDBOX"));
    }

    @Test
    @DisplayName("POST: ungueltiger profileKey-Regex -> 400")
    void keyRegexInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/llm-model-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileKey": "lowercase",
                                  "provider": "CLAUDE_CLOUD",
                                  "modelId": "claude-sonnet-4-6",
                                  "costBudgetEurMonthly": 10.00,
                                  "approvedForGkvData": false,
                                  "approvedBy": "a.admin@ahs.test"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST: negatives Budget -> 400")
    void negativesBudget() throws Exception {
        mockMvc.perform(post("/api/v1/llm-model-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileKey": "CLAUDE_BAD",
                                  "provider": "CLAUDE_CLOUD",
                                  "modelId": "claude-sonnet-4-6",
                                  "costBudgetEurMonthly": -1.00,
                                  "approvedForGkvData": false,
                                  "approvedBy": "a.admin@ahs.test"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
