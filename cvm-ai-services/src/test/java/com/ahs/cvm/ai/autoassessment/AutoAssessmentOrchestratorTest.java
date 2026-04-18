package com.ahs.cvm.ai.autoassessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.ai.rag.RetrievalService;
import com.ahs.cvm.ai.rag.RetrievalService.RagHit;
import com.ahs.cvm.application.cascade.CascadeInput;
import com.ahs.cvm.application.cascade.CascadeOutcome;
import com.ahs.cvm.application.rules.RuleEvaluationContext;
import com.ahs.cvm.application.rules.RuleEvaluationContext.ComponentSnapshot;
import com.ahs.cvm.application.rules.RuleEvaluationContext.CveSnapshot;
import com.ahs.cvm.application.rules.RuleEvaluationContext.FindingSnapshot;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AiCallStatus;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.llm.AiCallAuditService;
import com.ahs.cvm.llm.LlmClient;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClient.TokenUsage;
import com.ahs.cvm.llm.LlmClientSelector;
import com.ahs.cvm.llm.prompt.PromptTemplateLoader;
import com.ahs.cvm.persistence.ai.AiCallAudit;
import com.ahs.cvm.persistence.ai.AiCallAuditRepository;
import com.ahs.cvm.persistence.ai.AiSourceRef;
import com.ahs.cvm.persistence.ai.AiSourceRefRepository;
import com.ahs.cvm.persistence.ai.AiSuggestion;
import com.ahs.cvm.persistence.ai.AiSuggestionRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AutoAssessmentOrchestratorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AutoAssessmentConfig config;
    private RetrievalService retrievalService;
    private AiCallAuditService auditService;
    private LlmClientSelector clientSelector;
    private PromptTemplateLoader promptLoader;
    private FindingRepository findingRepository;
    private AiSuggestionRepository suggestionRepository;
    private AiSourceRefRepository sourceRefRepository;
    private AiCallAuditRepository auditRepository;
    private LlmClient llmClient;
    private AutoAssessmentOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        config = new AutoAssessmentConfig(true, 5, 0.6);
        retrievalService = mock(RetrievalService.class);
        auditService = mock(AiCallAuditService.class);
        clientSelector = mock(LlmClientSelector.class);
        promptLoader = new PromptTemplateLoader();
        findingRepository = mock(FindingRepository.class);
        suggestionRepository = mock(AiSuggestionRepository.class);
        sourceRefRepository = mock(AiSourceRefRepository.class);
        auditRepository = mock(AiCallAuditRepository.class);
        llmClient = mock(LlmClient.class);
        given(llmClient.modelId()).willReturn("claude-sonnet-4-6");
        given(clientSelector.select(any(), anyString())).willReturn(llmClient);
        given(suggestionRepository.save(any(AiSuggestion.class)))
                .willAnswer(inv -> {
                    AiSuggestion s = inv.getArgument(0);
                    s.setId(UUID.randomUUID());
                    return s;
                });
        AiCallAudit audit = AiCallAudit.builder()
                .id(UUID.randomUUID())
                .useCase("AUTO_ASSESSMENT")
                .modelId("claude-sonnet-4-6")
                .promptTemplateId("auto-assessment")
                .promptTemplateVersion("v1")
                .systemPrompt("s").userPrompt("u")
                .triggeredBy("system:auto-assessment")
                .injectionRisk(false)
                .status(AiCallStatus.OK)
                .createdAt(Instant.now())
                .build();
        given(auditRepository.findByStatusAndCreatedAtBefore(any(), any()))
                .willReturn(List.of(audit));

        orchestrator = new AutoAssessmentOrchestrator(
                config, retrievalService, auditService, clientSelector,
                promptLoader, findingRepository, suggestionRepository,
                sourceRefRepository, auditRepository);
    }

    private CascadeInput input(double cvss) {
        UUID findingId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        CveSnapshot cve = new CveSnapshot(
                UUID.randomUUID(), "CVE-2025-48924", "test summary",
                List.of("CWE-79"), false, BigDecimal.ZERO,
                BigDecimal.valueOf(cvss));
        ComponentSnapshot comp = new ComponentSnapshot("maven",
                "spring-core", "6.1.0");
        FindingSnapshot fs = new FindingSnapshot(findingId, Instant.now());
        RuleEvaluationContext ctx = new RuleEvaluationContext(
                cve, JsonNodeFactory.instance.objectNode(), comp, fs);
        return new CascadeInput(cve.id(),
                UUID.fromString("00000000-0000-0000-0000-0000000000aa"),
                UUID.fromString("00000000-0000-0000-0000-0000000000bb"),
                ctx);
    }

    private Finding fakeFinding() {
        Cve cve = Cve.builder()
                .id(UUID.randomUUID()).cveId("CVE-2025-48924")
                .summary("Sicherheitsluecke in spring-core").build();
        Finding f = Finding.builder().id(UUID.fromString(
                "11111111-1111-1111-1111-111111111111")).cve(cve).build();
        f.setFixedInVersion("6.1.5");
        return f;
    }

    private LlmResponse responseWith(String body) throws Exception {
        JsonNode node = MAPPER.readTree(body);
        return new LlmResponse(node, body, new TokenUsage(100, 30),
                Duration.ofMillis(120), "claude-sonnet-4-6");
    }

    @Test
    @DisplayName("AI-Cascade: deaktivierte Konfiguration liefert Optional.empty()")
    void deaktiviert() {
        config = new AutoAssessmentConfig(false, 5, 0.6);
        orchestrator = new AutoAssessmentOrchestrator(
                config, retrievalService, auditService, clientSelector,
                promptLoader, findingRepository, suggestionRepository,
                sourceRefRepository, auditRepository);

        Optional<CascadeOutcome> res = orchestrator.suggest(input(7.5));

        assertThat(res).isEmpty();
        verify(auditService, never()).execute(any(), any());
    }

    @Test
    @DisplayName("AI-Cascade: Happy-Path liefert AI-Outcome mit PROPOSED-Status")
    void happyPath() throws Exception {
        Finding f = fakeFinding();
        given(findingRepository.findById(f.getId())).willReturn(Optional.of(f));
        given(retrievalService.similar(any(), anyString(), anyInt())).willReturn(List.of(
                new RagHit(UUID.randomUUID(), "ASSESSMENT", "ref-1", 0,
                        "aehnliche Bewertung HIGH", "fake-1536",
                        Instant.now(), 0.85)));
        given(auditService.execute(any(), any())).willReturn(responseWith("""
                {
                  "severity":"HIGH","rationale":"Pfad ueber Spring",
                  "confidence":0.9,
                  "usedProfileFields":["architecture.windows_hosts"],
                  "sources":[{"kind":"PROFILE_PATH","ref":"architecture.windows_hosts","excerpt":"true"}],
                  "proposedFixVersion":"6.1.5"
                }"""));

        Optional<CascadeOutcome> res = orchestrator.suggest(input(8.5));

        assertThat(res).isPresent();
        CascadeOutcome out = res.get();
        assertThat(out.source()).isEqualTo(ProposalSource.AI_SUGGESTION);
        assertThat(out.severity()).isEqualTo(AhsSeverity.HIGH);
        assertThat(out.targetStatus()).isEqualTo(AssessmentStatus.PROPOSED);
        assertThat(out.aiSuggestionId()).isNotNull();
        assertThat(out.confidence()).isEqualByComparingTo("0.900");
        verify(suggestionRepository).save(any(AiSuggestion.class));
        verify(sourceRefRepository).save(any(AiSourceRef.class));
    }

    @Test
    @DisplayName("AI-Cascade: ohne starke RAG-Treffer bleibt Severity auf Original-Wert")
    void konservativerDefault() throws Exception {
        Finding f = fakeFinding();
        given(findingRepository.findById(f.getId())).willReturn(Optional.of(f));
        // Schwache RAG-Treffer (<0.6) und keine Profil-Felder verwendet.
        given(retrievalService.similar(any(), anyString(), anyInt())).willReturn(List.of(
                new RagHit(UUID.randomUUID(), "ASSESSMENT", "ref", 0,
                        "schwacher treffer", "fake-1536", Instant.now(), 0.3)));
        // LLM versucht ein Downgrade auf LOW.
        given(auditService.execute(any(), any())).willReturn(responseWith("""
                {
                  "severity":"LOW","rationale":"Kein Pfad",
                  "confidence":0.4,
                  "usedProfileFields":[],
                  "sources":[],
                  "proposedFixVersion":null
                }"""));

        // Original-Severity nach CVSS-Mapping fuer 8.5 ist HIGH.
        Optional<CascadeOutcome> res = orchestrator.suggest(input(8.5));

        assertThat(res).isPresent();
        assertThat(res.get().severity())
                .as("konservativer Default schuetzt vor Downgrade")
                .isEqualTo(AhsSeverity.HIGH);
        assertThat(res.get().rationale()).contains("Datenlage zu duenn");
    }

    @Test
    @DisplayName("AI-Cascade: Halluzinations-Verdacht setzt Status NEEDS_VERIFICATION")
    void halluzination() throws Exception {
        Finding f = fakeFinding();
        f.setFixedInVersion("6.1.5");
        given(findingRepository.findById(f.getId())).willReturn(Optional.of(f));
        given(retrievalService.similar(any(), anyString(), anyInt())).willReturn(List.of(
                new RagHit(UUID.randomUUID(), "ASSESSMENT", "ref", 0,
                        "starker treffer", "fake-1536", Instant.now(), 0.9)));
        // LLM erfindet eine Fix-Version, die nicht zur Advisory passt.
        given(auditService.execute(any(), any())).willReturn(responseWith("""
                {
                  "severity":"MEDIUM","rationale":"Patch verfuegbar",
                  "confidence":0.8,
                  "usedProfileFields":["architecture.x"],
                  "sources":[],
                  "proposedFixVersion":"99.99.99"
                }"""));

        Optional<CascadeOutcome> res = orchestrator.suggest(input(7.5));

        assertThat(res).isPresent();
        assertThat(res.get().targetStatus())
                .isEqualTo(AssessmentStatus.NEEDS_VERIFICATION);
    }

    @Test
    @DisplayName("AI-Cascade: technischer Fehler im Audit-Service -> Optional.empty()")
    void fehlerImService() {
        Finding f = fakeFinding();
        given(findingRepository.findById(f.getId())).willReturn(Optional.of(f));
        given(retrievalService.similar(any(), anyString(), anyInt())).willReturn(List.of());
        given(auditService.execute(any(), any()))
                .willThrow(new RuntimeException("timeout"));

        Optional<CascadeOutcome> res = orchestrator.suggest(input(5.0));

        assertThat(res).isEmpty();
    }

    @Test
    @DisplayName("AI-Cascade: Halluzinations-Check passt - kein NEEDS_VERIFICATION wenn Fix-Version stimmt")
    void halluzinationKeinTreffer() throws Exception {
        Finding f = fakeFinding();
        f.setFixedInVersion("6.1.5");
        given(findingRepository.findById(f.getId())).willReturn(Optional.of(f));
        given(retrievalService.similar(any(), anyString(), anyInt())).willReturn(List.of(
                new RagHit(UUID.randomUUID(), "ASSESSMENT", "ref", 0,
                        "starker treffer", "fake-1536", Instant.now(), 0.9)));
        given(auditService.execute(any(), any())).willReturn(responseWith("""
                {
                  "severity":"MEDIUM","rationale":"konsistent",
                  "confidence":0.7,
                  "usedProfileFields":["architecture.x"],
                  "sources":[],
                  "proposedFixVersion":"6.1.5"
                }"""));

        Optional<CascadeOutcome> res = orchestrator.suggest(input(7.5));

        assertThat(res).isPresent();
        assertThat(res.get().targetStatus()).isEqualTo(AssessmentStatus.PROPOSED);
    }

    @Test
    @DisplayName("AI-Cascade: HalluzinationsCheck - fixVersion aber keine Advisory-Version -> verdaechtig")
    void halluzinationOhneAdvisoryVersion() throws Exception {
        Finding f = fakeFinding();
        f.setFixedInVersion(null);
        given(findingRepository.findById(f.getId())).willReturn(Optional.of(f));
        given(retrievalService.similar(any(), anyString(), anyInt())).willReturn(List.of(
                new RagHit(UUID.randomUUID(), "ASSESSMENT", "ref", 0,
                        "starker treffer", "fake-1536", Instant.now(), 0.9)));
        given(auditService.execute(any(), any())).willReturn(responseWith("""
                {
                  "severity":"MEDIUM","rationale":"r",
                  "confidence":0.7,
                  "usedProfileFields":["x"],
                  "sources":[],
                  "proposedFixVersion":"1.2.3"
                }"""));

        Optional<CascadeOutcome> res = orchestrator.suggest(input(7.5));

        assertThat(res.get().targetStatus())
                .isEqualTo(AssessmentStatus.NEEDS_VERIFICATION);
    }
}
