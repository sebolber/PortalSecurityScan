package com.ahs.cvm.ai.reachability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.domain.enums.AiCallStatus;
import com.ahs.cvm.llm.audit.AiCallAuditPort;
import com.ahs.cvm.llm.prompt.PromptTemplateLoader;
import com.ahs.cvm.llm.subprocess.FakeSubprocessRunner;
import com.ahs.cvm.llm.subprocess.SubprocessRunner.SubprocessResult;
import com.ahs.cvm.persistence.ai.AiCallAudit;
import com.ahs.cvm.persistence.ai.AiCallAuditRepository;
import com.ahs.cvm.persistence.ai.AiSourceRef;
import com.ahs.cvm.persistence.ai.AiSourceRefRepository;
import com.ahs.cvm.persistence.ai.AiSuggestion;
import com.ahs.cvm.persistence.ai.AiSuggestionRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReachabilityAgentTest {

    private static final UUID FINDING_ID = UUID.fromString(
            "11111111-1111-1111-1111-111111111111");
    private static final UUID AUDIT_ID = UUID.fromString(
            "aa000000-0000-0000-0000-000000000000");

    private FakeSubprocessRunner runner;
    private GitCheckoutPort checkout;
    private FindingRepository findingRepository;
    private AiSuggestionRepository suggestionRepository;
    private AiSourceRefRepository sourceRefRepository;
    private AiCallAuditPort auditPort;
    private AiCallAuditRepository auditRepository;
    private ReachabilityAgent agent;

    @BeforeEach
    void setUp() {
        runner = new FakeSubprocessRunner();
        checkout = mock(GitCheckoutPort.class);
        findingRepository = mock(FindingRepository.class);
        suggestionRepository = mock(AiSuggestionRepository.class);
        sourceRefRepository = mock(AiSourceRefRepository.class);
        auditPort = mock(AiCallAuditPort.class);
        auditRepository = mock(AiCallAuditRepository.class);

        Cve cve = Cve.builder().id(UUID.randomUUID()).cveId("CVE-X").build();
        Finding f = Finding.builder().id(FINDING_ID).cve(cve).build();
        given(findingRepository.findById(FINDING_ID)).willReturn(Optional.of(f));
        given(checkout.checkout(any(), any(), any())).willReturn(
                Path.of(System.getProperty("java.io.tmpdir")));
        given(auditPort.persistPending(any())).willReturn(AUDIT_ID);
        AiCallAudit audit = AiCallAudit.builder()
                .id(AUDIT_ID).useCase("REACHABILITY").modelId("claude-code-cli")
                .promptTemplateId("reachability").promptTemplateVersion("v1")
                .systemPrompt("s").userPrompt("u").triggeredBy("t@x")
                .injectionRisk(false).status(AiCallStatus.OK)
                .createdAt(Instant.now()).build();
        given(auditRepository.findById(AUDIT_ID)).willReturn(Optional.of(audit));
        given(suggestionRepository.save(any(AiSuggestion.class)))
                .willAnswer(inv -> {
                    AiSuggestion s = inv.getArgument(0);
                    s.setId(UUID.randomUUID());
                    return s;
                });
        agent = new ReachabilityAgent(
                new ReachabilityConfig(true, 60, "claude"),
                checkout, runner, new PromptTemplateLoader(),
                findingRepository, suggestionRepository, sourceRefRepository,
                auditPort, auditRepository);
    }

    private ReachabilityRequest req() {
        return new ReachabilityRequest(
                FINDING_ID, "ssh://git@example/cvm.git", "main",
                "abc1234", "com.example.X.y(Type)", "java",
                "kontext", "t.tester@ahs.test");
    }

    @Test
    @DisplayName("Reachability: Analyse findet drei Call-Sites und klassifiziert alle als statische Konfiguration")
    void dreiCallSites() {
        runner.setResponseFactory(r -> new SubprocessResult(0, """
                {"findings":{"callSites":[
                  {"file":"a.java","line":1,"symbol":"X.y","trust":"STATIC_CONFIG","note":"a"},
                  {"file":"b.java","line":2,"symbol":"X.y","trust":"STATIC_CONFIG","note":"b"},
                  {"file":"c.java","line":3,"symbol":"X.y","trust":"STATIC_CONFIG","note":"c"}
                ]},"summary":"3 Call-Sites","recommendation":"ACCEPT"}
                """, "", 50L, false));

        ReachabilityResult res = agent.analyze(req());

        assertThat(res.available()).isTrue();
        assertThat(res.recommendation()).isEqualTo("ACCEPT");
        assertThat(res.callSites()).hasSize(3);
        assertThat(res.callSites()).allMatch(c -> "STATIC_CONFIG".equals(c.trust()));
        verify(suggestionRepository).save(any(AiSuggestion.class));
        verify(sourceRefRepository, times(3)).save(any(AiSourceRef.class));
    }

    @Test
    @DisplayName("Reachability: deaktiviert -> kein Subprocess-Aufruf, Result.available=false")
    void deaktiviert() {
        agent = new ReachabilityAgent(
                new ReachabilityConfig(false, 60, "claude"),
                checkout, runner, new PromptTemplateLoader(),
                findingRepository, suggestionRepository, sourceRefRepository,
                auditPort, auditRepository);

        ReachabilityResult res = agent.analyze(req());

        assertThat(res.available()).isFalse();
        assertThat(res.noteIfUnavailable()).contains("deaktiviert");
        verify(auditPort, never()).persistPending(any());
    }

    @Test
    @DisplayName("Reachability: Subprocess-Timeout -> Audit ERROR + Result.available=false")
    void timeout() {
        runner.setResponseFactory(r -> new SubprocessResult(
                -1, "", "timeout", 200L, true));

        ReachabilityResult res = agent.analyze(req());

        assertThat(res.available()).isFalse();
        assertThat(res.noteIfUnavailable()).contains("Timeout");
        ArgumentCaptor<AiCallAuditPort.AiCallAuditFinalization> cap =
                ArgumentCaptor.forClass(AiCallAuditPort.AiCallAuditFinalization.class);
        verify(auditPort).finalize(any(UUID.class), cap.capture());
        assertThat(cap.getValue().status()).isEqualTo(AiCallStatus.ERROR);
    }

    @Test
    @DisplayName("Reachability: ungueltiges JSON -> Audit INVALID_OUTPUT + Result.available=false")
    void invalidOutput() {
        runner.setResponseFactory(r -> new SubprocessResult(
                0, "no-json", "", 50L, false));

        ReachabilityResult res = agent.analyze(req());

        assertThat(res.available()).isFalse();
        ArgumentCaptor<AiCallAuditPort.AiCallAuditFinalization> cap =
                ArgumentCaptor.forClass(AiCallAuditPort.AiCallAuditFinalization.class);
        verify(auditPort).finalize(any(UUID.class), cap.capture());
        assertThat(cap.getValue().status()).isEqualTo(AiCallStatus.INVALID_OUTPUT);
    }

    @Test
    @DisplayName("Reachability: JSON ohne callSites-Array -> INVALID_OUTPUT")
    void schemaVerletzung() {
        runner.setResponseFactory(r -> new SubprocessResult(
                0, "{\"summary\":\"x\"}", "", 50L, false));

        ReachabilityResult res = agent.analyze(req());

        assertThat(res.available()).isFalse();
        assertThat(res.noteIfUnavailable()).contains("Schema");
    }

    @Test
    @DisplayName("Reachability: Subprocess-Aufruf enthaelt --read-only und --output json")
    void subprocessFlags() {
        runner.setResponseFactory(r -> new SubprocessResult(0,
                "{\"findings\":{\"callSites\":[]},\"recommendation\":\"VERIFY\",\"summary\":\"\"}",
                "", 10L, false));

        agent.analyze(req());

        assertThat(runner.aufrufe()).hasSize(1);
        assertThat(runner.aufrufe().get(0).command())
                .contains("--read-only")
                .contains("--output").contains("json");
    }
}
