package com.ahs.cvm.ai.profileassistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.ai.profileassistant.ProfileAssistantService.FinalizeResult;
import com.ahs.cvm.ai.profileassistant.ProfileAssistantService.StartResult;
import com.ahs.cvm.application.profile.ContextProfileService;
import com.ahs.cvm.application.profile.ProfileView;
import com.ahs.cvm.domain.enums.ProfileState;
import com.ahs.cvm.llm.AiCallAuditService;
import com.ahs.cvm.llm.LlmClient;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClient.TokenUsage;
import com.ahs.cvm.llm.LlmClientSelector;
import com.ahs.cvm.llm.prompt.PromptTemplateLoader;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import com.ahs.cvm.persistence.profileassist.ProfileAssistSession;
import com.ahs.cvm.persistence.profileassist.ProfileAssistSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileAssistantServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final UUID ENV_ID = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    private ProfileAssistSessionRepository sessionRepo;
    private EnvironmentRepository envRepo;
    private AssessmentRepository assessmentRepo;
    private ContextProfileService profileService;
    private AiCallAuditService auditService;
    private LlmClientSelector selector;
    private ProfileAssistantService service;
    private ProfileAssistantConfig config;

    @BeforeEach
    void setUp() {
        sessionRepo = mock(ProfileAssistSessionRepository.class);
        envRepo = mock(EnvironmentRepository.class);
        assessmentRepo = mock(AssessmentRepository.class);
        profileService = mock(ContextProfileService.class);
        auditService = mock(AiCallAuditService.class);
        selector = mock(LlmClientSelector.class);
        LlmClient client = mock(LlmClient.class);
        given(client.modelId()).willReturn("claude-sonnet-4-6");
        given(selector.select(any(), anyString())).willReturn(client);
        Environment env = Environment.builder().id(ENV_ID).key("REF-TEST").build();
        given(envRepo.findById(ENV_ID)).willReturn(Optional.of(env));
        given(sessionRepo.save(any(ProfileAssistSession.class))).willAnswer(inv -> {
            ProfileAssistSession s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });
        given(assessmentRepo.findAll()).willReturn(List.of());
        config = new ProfileAssistantConfig(true, 24);
        service = new ProfileAssistantService(
                config, sessionRepo, envRepo, assessmentRepo, profileService,
                auditService, selector, new PromptTemplateLoader());
    }

    private LlmResponse res(String q) throws Exception {
        JsonNode n = MAPPER.readTree(
                "{\"question\":\"" + q + "\",\"fieldPath\":\"architecture.x\","
                        + "\"answerType\":\"boolean\",\"options\":[]}");
        return new LlmResponse(n, "", new TokenUsage(10, 10),
                Duration.ofMillis(50), "claude-sonnet-4-6");
    }

    @Test
    @DisplayName("Start: deaktiviert -> IllegalStateException")
    void deaktiviert() {
        service = new ProfileAssistantService(
                new ProfileAssistantConfig(false, 24),
                sessionRepo, envRepo, assessmentRepo, profileService,
                auditService, selector, new PromptTemplateLoader());
        assertThatThrownBy(() -> service.start(ENV_ID, "a.admin@ahs.test"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Start: erzeugt Session mit pendingQuestion aus LLM")
    void startOk() throws Exception {
        given(auditService.execute(any(), any())).willReturn(res("Hat die Umgebung Windows-Hosts?"));

        StartResult r = service.start(ENV_ID, "a.admin@ahs.test");

        assertThat(r.question()).contains("Windows-Hosts");
        assertThat(r.expiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Reply: haengt Antwort an Dialog und liefert naechste Frage")
    void reply() throws Exception {
        given(auditService.execute(any(), any()))
                .willReturn(res("Frage 1"))
                .willReturn(res("Frage 2"));
        StartResult start = service.start(ENV_ID, "u@x");
        given(sessionRepo.findById(start.sessionId()))
                .willReturn(Optional.of(fakeSession(start.sessionId(), Instant.now().plusSeconds(3600))));

        var rep = service.reply(start.sessionId(), "architecture.x", "ja");

        assertThat(rep.nextQuestion()).contains("Frage 2");
    }

    @Test
    @DisplayName("Finalize: erzeugt Draft via ContextProfileService.proposeNewVersion (kein Direkt-Schreib)")
    void finalizeErzeugtDraft() throws Exception {
        given(auditService.execute(any(), any())).willReturn(res("F"));
        StartResult start = service.start(ENV_ID, "u@x");
        given(sessionRepo.findById(start.sessionId()))
                .willReturn(Optional.of(fakeSession(start.sessionId(), Instant.now().plusSeconds(3600))));
        UUID draftId = UUID.randomUUID();
        given(profileService.proposeNewVersion(eq(ENV_ID), any(), eq("a.admin@ahs.test")))
                .willReturn(new ProfileView(
                        draftId, ENV_ID, 2, ProfileState.DRAFT, "yaml",
                        "u@x", null, null, Instant.now()));

        FinalizeResult r = service.finalizeDraft(start.sessionId(), "a.admin@ahs.test");

        assertThat(r.draftProfileId()).isEqualTo(draftId);
        verify(profileService).proposeNewVersion(eq(ENV_ID), any(), eq("a.admin@ahs.test"));
    }

    @Test
    @DisplayName("Reply: abgelaufene Session wirft IllegalStateException und markiert EXPIRED")
    void timeout() throws Exception {
        given(auditService.execute(any(), any())).willReturn(res("F"));
        StartResult start = service.start(ENV_ID, "u@x");
        ProfileAssistSession session = fakeSession(start.sessionId(),
                Instant.now().minusSeconds(60));
        given(sessionRepo.findById(start.sessionId())).willReturn(Optional.of(session));

        assertThatThrownBy(() -> service.reply(start.sessionId(), "x", "y"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(session.getStatus()).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("Finalize: Service ruft NIEMALS einen Direkt-Schreibpfad am aktiven Profil")
    void keinDirektSchreib() throws Exception {
        given(auditService.execute(any(), any())).willReturn(res("F"));
        StartResult start = service.start(ENV_ID, "u@x");
        given(sessionRepo.findById(start.sessionId()))
                .willReturn(Optional.of(fakeSession(start.sessionId(), Instant.now().plusSeconds(3600))));
        given(profileService.proposeNewVersion(any(), any(), any()))
                .willReturn(new ProfileView(
                        UUID.randomUUID(), ENV_ID, 2, ProfileState.DRAFT, "y",
                        "u@x", null, null, Instant.now()));

        service.finalizeDraft(start.sessionId(), "a.admin@ahs.test");

        // Der Service darf nur proposeNewVersion ansprechen - weder approve,
        // latestActiveFor, noch diff. Pruefen via Mockito (keine anderen
        // Methodenaufrufe).
        org.mockito.Mockito.verify(profileService).proposeNewVersion(any(), any(), any());
        org.mockito.Mockito.verifyNoMoreInteractions(profileService);
    }

    private ProfileAssistSession fakeSession(UUID id, Instant expires) {
        Environment env = Environment.builder().id(ENV_ID).key("REF-TEST").build();
        return ProfileAssistSession.builder()
                .id(id).environment(env).startedBy("u@x")
                .dialogJson("[]").status("ACTIVE")
                .createdAt(Instant.now()).expiresAt(expires).build();
    }
}
