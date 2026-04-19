package com.ahs.cvm.api.profile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.profile.ContextProfileService;
import com.ahs.cvm.application.profile.FourEyesViolationException;
import com.ahs.cvm.application.profile.ProfileFieldDiff;
import com.ahs.cvm.application.profile.ProfileValidationException;
import com.ahs.cvm.application.profile.ProfileView;
import com.ahs.cvm.domain.enums.ProfileState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
        controllers = ProfileController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(ProfileExceptionHandler.class)
class ProfileControllerWebTest {

    @Autowired MockMvc mockMvc;

    @MockBean ContextProfileService profileService;

    private static final UUID ENV_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DRAFT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    @DisplayName("GET /environments/{id}/profile: 200 mit aktueller Version")
    void holtAktuellesProfil() throws Exception {
        ProfileView view = neueView(ProfileState.ACTIVE);
        given(profileService.latestActiveFor(ENV_ID)).willReturn(Optional.of(view));

        mockMvc.perform(get("/api/v1/environments/{id}/profile", ENV_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andExpect(jsonPath("$.versionNumber").value(4));
    }

    @Test
    @DisplayName("GET /environments/{id}/profile: 404, wenn keine aktive Version existiert")
    void keinProfilVorhanden() throws Exception {
        given(profileService.latestActiveFor(ENV_ID)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/environments/{id}/profile", ENV_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /environments/{id}/profile/draft: 200 mit DRAFT-Version")
    void aktuellerDraftLiefert200() throws Exception {
        ProfileView draft = neueView(ProfileState.DRAFT);
        given(profileService.latestDraftFor(ENV_ID)).willReturn(Optional.of(draft));

        mockMvc.perform(get("/api/v1/environments/{id}/profile/draft", ENV_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("DRAFT"))
                .andExpect(jsonPath("$.versionNumber").value(4));
    }

    @Test
    @DisplayName("GET /environments/{id}/profile/draft: 404, wenn kein DRAFT existiert")
    void aktuellerDraftLiefert404() throws Exception {
        given(profileService.latestDraftFor(ENV_ID)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/environments/{id}/profile/draft", ENV_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /environments/{id}/profile: legt Draft an und liefert 201 Created")
    void draftAnlegen() throws Exception {
        ProfileView draft = neueView(ProfileState.DRAFT);
        given(profileService.proposeNewVersion(eq(ENV_ID), any(), eq("t.tester@ahs.test")))
                .willReturn(draft);

        String body =
                """
                {
                  "yamlSource": "schemaVersion: 1\\numgebung:\\n  key: REF\\n  stage: REF\\n",
                  "proposedBy": "t.tester@ahs.test"
                }
                """;

        mockMvc.perform(put("/api/v1/environments/{id}/profile", ENV_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("DRAFT"));
    }

    @Test
    @DisplayName("PUT /environments/{id}/profile: 400 bei ProfileValidationException")
    void draftSchemaFehler() throws Exception {
        willThrow(new ProfileValidationException("Pflichtfeld fehlt: umgebung.key"))
                .given(profileService)
                .proposeNewVersion(any(), any(), any());

        String body = "{\"yamlSource\":\"schemaVersion: 1\",\"proposedBy\":\"t.tester@ahs.test\"}";

        mockMvc.perform(put("/api/v1/environments/{id}/profile", ENV_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("profile_validation_error"));
    }

    @Test
    @DisplayName(
            "GET /profiles/{id}/diff?against=latest: 200 + leere Liste, wenn noch keine aktive Vorgaenger-Version existiert")
    void diffLiefertLeereListeOhneAktiv() throws Exception {
        given(profileService.environmentOf(DRAFT_ID)).willReturn(Optional.of(ENV_ID));
        given(profileService.latestActiveFor(ENV_ID)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/profiles/{id}/diff", DRAFT_ID).param("against", "latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName(
            "GET /profiles/{id}/diff?against=latest: 404, wenn die Profil-ID selbst unbekannt ist")
    void diffLiefert404BeiUnbekannterProfilId() throws Exception {
        given(profileService.environmentOf(DRAFT_ID)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/profiles/{id}/diff", DRAFT_ID).param("against", "latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("profile_not_found"));
    }

    @Test
    @DisplayName(
            "GET /profiles/{id}/diff?against=latest: 200 mit echten Diff-Eintraegen, wenn eine aktive Vorgaenger-Version existiert")
    void diffLiefertEintraegeGegenAktiv() throws Exception {
        UUID altId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        ProfileView aktiv =
                new ProfileView(
                        altId,
                        ENV_ID,
                        3,
                        ProfileState.ACTIVE,
                        "schemaVersion: 1\n",
                        "t.tester@ahs.test",
                        "a.admin@ahs.test",
                        Instant.now(),
                        Instant.now());
        given(profileService.environmentOf(DRAFT_ID)).willReturn(Optional.of(ENV_ID));
        given(profileService.latestActiveFor(ENV_ID)).willReturn(Optional.of(aktiv));
        given(profileService.diff(altId, DRAFT_ID)).willReturn(List.<ProfileFieldDiff>of());

        mockMvc.perform(get("/api/v1/profiles/{id}/diff", DRAFT_ID).param("against", "latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /profiles/{id}/approve: 409 bei Vier-Augen-Verstoss")
    void approveVierAugenVerstoss() throws Exception {
        willThrow(new FourEyesViolationException("Approver identisch mit Autor"))
                .given(profileService)
                .approve(eq(DRAFT_ID), eq("t.tester@ahs.test"));

        mockMvc.perform(post("/api/v1/profiles/{id}/approve", DRAFT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approverId\":\"t.tester@ahs.test\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("four_eyes_violation"));
    }

    private ProfileView neueView(ProfileState state) {
        return new ProfileView(
                DRAFT_ID,
                ENV_ID,
                4,
                state,
                "schemaVersion: 1\numgebung:\n  key: REF-TEST\n  stage: REF\n",
                "t.tester@ahs.test",
                state == ProfileState.ACTIVE ? "a.admin@ahs.test" : null,
                state == ProfileState.ACTIVE ? Instant.now() : null,
                Instant.now());
    }
}
