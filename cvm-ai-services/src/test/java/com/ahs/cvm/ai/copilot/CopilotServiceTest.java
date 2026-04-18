package com.ahs.cvm.ai.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.ai.rag.RetrievalService;
import com.ahs.cvm.ai.rag.RetrievalService.RagHit;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.llm.AiCallAuditService;
import com.ahs.cvm.llm.LlmClient;
import com.ahs.cvm.llm.LlmClient.LlmRequest;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClient.TokenUsage;
import com.ahs.cvm.llm.LlmClientSelector;
import com.ahs.cvm.llm.prompt.PromptTemplateLoader;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.environment.Environment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CopilotServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final UUID ASSESSMENT_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ENV_ID =
            UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    private AssessmentRepository assessmentRepository;
    private RetrievalService retrievalService;
    private AiCallAuditService auditService;
    private LlmClientSelector clientSelector;
    private CopilotService service;

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        retrievalService = mock(RetrievalService.class);
        auditService = mock(AiCallAuditService.class);
        clientSelector = mock(LlmClientSelector.class);
        LlmClient client = mock(LlmClient.class);
        given(client.modelId()).willReturn("claude-sonnet-4-6");
        given(clientSelector.select(any(), anyString())).willReturn(client);
        given(assessmentRepository.findById(ASSESSMENT_ID))
                .willReturn(Optional.of(fakeAssessment()));
        service = new CopilotService(
                assessmentRepository, retrievalService, auditService,
                clientSelector, new PromptTemplateLoader());
    }

    private Assessment fakeAssessment() {
        Cve cve = Cve.builder().id(UUID.randomUUID())
                .cveId("CVE-2025-48924").build();
        Environment env = Environment.builder().id(ENV_ID).build();
        return Assessment.builder()
                .id(ASSESSMENT_ID)
                .cve(cve)
                .environment(env)
                .severity(AhsSeverity.MEDIUM)
                .status(AssessmentStatus.PROPOSED)
                .proposalSource(ProposalSource.HUMAN)
                .rationale("aktueller Text")
                .build();
    }

    private LlmResponse responseWith(String json) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        return new LlmResponse(node, json, new TokenUsage(50, 20),
                Duration.ofMillis(120), "claude-sonnet-4-6");
    }

    private CopilotRequest req(CopilotUseCase uc, Map<String, String> attachments) {
        return new CopilotRequest(
                ASSESSMENT_ID, uc, "bitte verfeinern", "t.tester@ahs.test",
                attachments == null ? Map.of() : attachments);
    }

    @Test
    @DisplayName("Copilot: REFINE_RATIONALE liefert nur text + sources, kein Severity-Feld")
    void refineRationale() throws Exception {
        given(auditService.execute(any(), any())).willReturn(responseWith("""
                {"text":"Praezise Begruendung.","sources":[]}"""));

        CopilotSuggestion s = service.suggest(req(CopilotUseCase.REFINE_RATIONALE, null));

        assertThat(s.text()).isEqualTo("Praezise Begruendung.");
        assertThat(s.useCase()).isEqualTo(CopilotUseCase.REFINE_RATIONALE);
        // Suggestion-Klasse hat kein Severity-Feld - Compile-Time-Garantie.
        assertThat(s.getClass().getDeclaredFields()).extracting("name")
                .doesNotContain("severity", "status");
    }

    @Test
    @DisplayName("Copilot: SIMILAR_ASSESSMENTS uebergibt RAG-Treffer in den Prompt")
    void similar() throws Exception {
        given(retrievalService.similar(any(), anyString(), anyInt())).willReturn(List.of(
                new RagHit(UUID.randomUUID(), "ASSESSMENT", "ref", 0,
                        "treffer1", "fake-1536", Instant.now(), 0.9)));
        given(auditService.execute(any(), any())).willReturn(responseWith("""
                {"text":"3 aehnliche Faelle gefunden.","sources":[
                  {"kind":"ASSESSMENT","ref":"a-1","excerpt":"..."}]}"""));

        CopilotSuggestion s = service.suggest(
                req(CopilotUseCase.SIMILAR_ASSESSMENTS, null));

        assertThat(s.sources()).hasSize(1);
        assertThat(s.sources().get(0).kind()).isEqualTo("ASSESSMENT");
        ArgumentCaptor<LlmRequest> cap = ArgumentCaptor.forClass(LlmRequest.class);
        verify(auditService).execute(any(), cap.capture());
        String prompt = cap.getValue().messages().get(0).content();
        assertThat(prompt).contains("treffer1");
    }

    @Test
    @DisplayName("Copilot: EXPLAIN_COMMIT injiziert das Commit-Attachment in den Prompt")
    void explainCommit() throws Exception {
        given(auditService.execute(any(), any())).willReturn(responseWith("""
                {"text":"Erklaerung","sources":[]}"""));

        service.suggest(req(CopilotUseCase.EXPLAIN_COMMIT,
                Map.of("commit", "diff --git a/x b/y\\n+ fix")));

        ArgumentCaptor<LlmRequest> cap = ArgumentCaptor.forClass(LlmRequest.class);
        verify(auditService).execute(any(), cap.capture());
        assertThat(cap.getValue().messages().get(0).content()).contains("diff --git");
    }

    @Test
    @DisplayName("Copilot: AUDIT_TONE rendert das aktuelle Rationale in den Prompt")
    void auditTone() throws Exception {
        given(auditService.execute(any(), any())).willReturn(responseWith("""
                {"text":"sachlich","sources":[]}"""));

        service.suggest(req(CopilotUseCase.AUDIT_TONE, null));

        ArgumentCaptor<LlmRequest> cap = ArgumentCaptor.forClass(LlmRequest.class);
        verify(auditService).execute(any(), cap.capture());
        assertThat(cap.getValue().messages().get(0).content())
                .contains("aktueller Text");
    }

    @Test
    @DisplayName("Copilot: Begruendungsverfeinerung uebernimmt Severity nicht")
    void severityWirdNiemalsGesetzt() throws Exception {
        // LLM versucht Severity ins JSON zu schreiben - Service muss es ignorieren.
        given(auditService.execute(any(), any())).willReturn(responseWith("""
                {"text":"x","severity":"CRITICAL","status":"APPROVED","sources":[]}"""));

        CopilotSuggestion s = service.suggest(req(CopilotUseCase.REFINE_RATIONALE, null));

        // Suggestion hat schlicht kein severity/status-Feld.
        assertThat(s.text()).isEqualTo("x");
        assertThat(s.getClass().getDeclaredFields()).extracting("name")
                .doesNotContain("severity", "status");
    }

    @Test
    @DisplayName("Copilot: leere Instruction wirft IllegalArgumentException")
    void leereInstruction() {
        assertThatThrownBy(() -> new CopilotRequest(
                ASSESSMENT_ID, CopilotUseCase.REFINE_RATIONALE,
                "  ", "t@x", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Copilot: unbekanntes Assessment wirft IllegalArgumentException")
    void unbekanntesAssessment() {
        UUID unbekannt = UUID.randomUUID();
        given(assessmentRepository.findById(unbekannt)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.suggest(new CopilotRequest(
                unbekannt, CopilotUseCase.AUDIT_TONE,
                "x", "t@x", Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
