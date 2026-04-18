package com.ahs.cvm.ai.fixverify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.ai.fixverify.FixVerificationService.FixVerificationRequest;
import com.ahs.cvm.domain.enums.AiCallStatus;
import com.ahs.cvm.domain.enums.FixEvidenceType;
import com.ahs.cvm.domain.enums.FixVerificationGrade;
import com.ahs.cvm.integration.git.GitProviderPort;
import com.ahs.cvm.integration.git.GitProviderPort.CommitSummary;
import com.ahs.cvm.integration.git.GitProviderPort.ReleaseNotes;
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
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.mitigation.MitigationPlan;
import com.ahs.cvm.persistence.mitigation.MitigationPlanRepository;
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

class FixVerificationServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final UUID MITIGATION_ID =
            UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String REPO = "https://github.com/foo/bar";

    private MitigationPlanRepository mitRepo;
    private GitProviderPort provider;
    private LlmClientSelector selector;
    private AiCallAuditService auditService;
    private AiCallAuditRepository auditRepo;
    private AiSuggestionRepository suggestionRepo;
    private AiSourceRefRepository sourceRefRepo;
    private FixVerificationService service;

    @BeforeEach
    void setUp() {
        mitRepo = mock(MitigationPlanRepository.class);
        provider = mock(GitProviderPort.class);
        selector = mock(LlmClientSelector.class);
        auditService = mock(AiCallAuditService.class);
        auditRepo = mock(AiCallAuditRepository.class);
        suggestionRepo = mock(AiSuggestionRepository.class);
        sourceRefRepo = mock(AiSourceRefRepository.class);
        LlmClient client = mock(LlmClient.class);
        given(client.modelId()).willReturn("claude-sonnet-4-6");
        given(selector.select(any(), anyString())).willReturn(client);
        given(auditRepo.findByStatusAndCreatedAtBefore(any(), any()))
                .willReturn(List.of(AiCallAudit.builder()
                        .id(UUID.randomUUID()).useCase("FIX_VERIFICATION")
                        .modelId("claude-sonnet-4-6")
                        .promptTemplateId("fix-verification").promptTemplateVersion("v1")
                        .systemPrompt("s").userPrompt("u")
                        .triggeredBy("t@x").injectionRisk(false)
                        .status(AiCallStatus.OK)
                        .createdAt(Instant.now()).build()));
        given(suggestionRepo.save(any(AiSuggestion.class))).willAnswer(inv -> {
            AiSuggestion s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        given(mitRepo.save(any(MitigationPlan.class))).willAnswer(inv -> inv.getArgument(0));
        given(mitRepo.findById(MITIGATION_ID)).willReturn(Optional.of(fakeMitigation()));

        service = new FixVerificationService(
                new FixVerificationConfig(true, 50, 1440),
                mitRepo, provider,
                new SuspiciousCommitHeuristic(),
                selector, auditService, auditRepo,
                suggestionRepo, sourceRefRepo,
                new PromptTemplateLoader());
    }

    private MitigationPlan fakeMitigation() {
        Cve cve = Cve.builder().id(UUID.randomUUID()).cveId("CVE-2025-48924").build();
        Assessment a = Assessment.builder()
                .id(UUID.randomUUID()).cve(cve)
                .build();
        MitigationPlan p = MitigationPlan.builder()
                .id(MITIGATION_ID).assessment(a).targetVersion("v1.14.3").build();
        return p;
    }

    private CommitSummary commit(String sha, String message, List<String> files) {
        return new CommitSummary(sha, message,
                "https://github.com/foo/bar/commit/" + sha,
                "a@x", Instant.now(), files);
    }

    private LlmResponse response(String json) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        return new LlmResponse(node, json, new TokenUsage(50, 30),
                Duration.ofMillis(80), "claude-sonnet-4-6");
    }

    private FixVerificationRequest req(String vulnerableSymbol) {
        return new FixVerificationRequest(
                MITIGATION_ID, REPO, "v1.14.2", "v1.14.3",
                vulnerableSymbol, "t.tester@ahs.test");
    }

    @Test
    @DisplayName("Fix-Verifikation: Grade A nur wenn CVE-ID in Release-Notes oder Commit-Message explizit vorkommt")
    void gradeA() throws Exception {
        given(provider.releaseNotes(REPO, "v1.14.3")).willReturn(Optional.of(
                new ReleaseNotes(REPO, "v1.14.3",
                        "Fix CVE-2025-48924 in Parser.", Instant.now(), REPO)));
        given(provider.compare(REPO, "v1.14.2", "v1.14.3")).willReturn(List.of(
                commit("abc", "fix(parser): patch for CVE-2025-48924", List.of("Parser.java"))));
        given(auditService.execute(any(), any())).willReturn(response("""
                {
                  "quality":"A","evidenceType":"EXPLICIT_CVE_MENTION",
                  "adressedBy":[{"commit":"abc","message":"fix","url":"u"}],
                  "confidence":0.9, "caveats":[]
                }"""));

        FixVerificationResult r = service.verify(req("Parser.parse(String)"));

        assertThat(r.grade()).isEqualTo(FixVerificationGrade.A);
        assertThat(r.evidenceType()).isEqualTo(FixEvidenceType.EXPLICIT_CVE_MENTION);
    }

    @Test
    @DisplayName("Fix-Verifikation: Service ueberschreibt Grade A, wenn CVE-ID in keiner Quelle belegt ist")
    void downgradeAaufBwennKeineCveId() throws Exception {
        given(provider.releaseNotes(REPO, "v1.14.3")).willReturn(Optional.of(
                new ReleaseNotes(REPO, "v1.14.3", "security improvements", Instant.now(), REPO)));
        given(provider.compare(REPO, "v1.14.2", "v1.14.3")).willReturn(List.of(
                commit("abc", "fix parser XXE issue", List.of("Parser.java"))));
        given(auditService.execute(any(), any())).willReturn(response("""
                {"quality":"A","evidenceType":"EXPLICIT_CVE_MENTION",
                 "adressedBy":[{"commit":"abc","message":"fix parser","url":"u"}],
                 "confidence":0.7,"caveats":[]}"""));

        FixVerificationResult r = service.verify(req("Parser.parse(String)"));

        assertThat(r.grade()).isEqualTo(FixVerificationGrade.B);
        assertThat(r.evidenceType()).isEqualTo(FixEvidenceType.FIX_COMMIT_MATCH);
        assertThat(r.caveats()).anyMatch(c -> c.contains("Service-Downgrade"));
    }

    @Test
    @DisplayName("Fix-Verifikation: Grade C wenn nur 'security improvements' ohne Keyword/Symbol-Match")
    void gradeC() throws Exception {
        given(provider.releaseNotes(REPO, "v1.14.3")).willReturn(Optional.of(
                new ReleaseNotes(REPO, "v1.14.3",
                        "general improvements", Instant.now(), REPO)));
        given(provider.compare(REPO, "v1.14.2", "v1.14.3")).willReturn(List.of(
                commit("abc", "chore: bump version", List.of("pom.xml"))));
        given(auditService.execute(any(), any())).willReturn(response("""
                {"quality":"C","evidenceType":"NONE","adressedBy":[],
                 "confidence":0.3,"caveats":["no clear evidence"]}"""));

        FixVerificationResult r = service.verify(req("Parser.parse(String)"));

        assertThat(r.grade()).isEqualTo(FixVerificationGrade.C);
        assertThat(r.evidenceType()).isEqualTo(FixEvidenceType.NONE);
    }

    @Test
    @DisplayName("Fix-Verifikation: Service-Upgrade auf A, wenn Commit CVE-ID trotz LLM-Grade C nennt")
    void upgradeCaufAwennCveIdBelegt() throws Exception {
        given(provider.releaseNotes(REPO, "v1.14.3")).willReturn(Optional.empty());
        given(provider.compare(REPO, "v1.14.2", "v1.14.3")).willReturn(List.of(
                commit("abc", "fix: addresses CVE-2025-48924", List.of())));
        given(auditService.execute(any(), any())).willReturn(response("""
                {"quality":"C","evidenceType":"NONE","adressedBy":[],
                 "confidence":0.2,"caveats":[]}"""));

        FixVerificationResult r = service.verify(req(null));

        assertThat(r.grade()).isEqualTo(FixVerificationGrade.A);
        assertThat(r.evidenceType()).isEqualTo(FixEvidenceType.EXPLICIT_CVE_MENTION);
    }

    @Test
    @DisplayName("Fix-Verifikation: deaktiviert -> available=false, kein Provider-/LLM-Call")
    void deaktiviert() {
        service = new FixVerificationService(
                new FixVerificationConfig(false, 50, 1440),
                mitRepo, provider,
                new SuspiciousCommitHeuristic(),
                selector, auditService, auditRepo,
                suggestionRepo, sourceRefRepo,
                new PromptTemplateLoader());

        FixVerificationResult r = service.verify(req(null));

        assertThat(r.available()).isFalse();
        assertThat(r.note()).contains("deaktiviert");
    }

    @Test
    @DisplayName("Fix-Verifikation: load liefert UNKNOWN, wenn noch nicht verifiziert")
    void loadUnverified() {
        FixVerificationResult r = service.load(MITIGATION_ID);
        assertThat(r.grade()).isEqualTo(FixVerificationGrade.UNKNOWN);
        assertThat(r.available()).isFalse();
    }
}
