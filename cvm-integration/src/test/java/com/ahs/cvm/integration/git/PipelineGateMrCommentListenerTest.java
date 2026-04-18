package com.ahs.cvm.integration.git;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.application.pipeline.PipelineGateEvaluatedEvent;
import com.ahs.cvm.application.pipeline.PipelineGateService.GateDecision;
import com.ahs.cvm.application.pipeline.PipelineGateService.GateResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PipelineGateMrCommentListenerTest {

    private static final UUID PV = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-04-18T10:00:00Z");

    private FakeGitProvider provider;

    @BeforeEach
    void setUp() {
        provider = new FakeGitProvider();
        provider.reset();
    }

    @Test
    @DisplayName("postet Gate-Kommentar mit PASS/WARN/FAIL-Icon")
    void postetKommentar() {
        PipelineGateMrCommentListener listener =
                new PipelineGateMrCommentListener(provider, true);
        GateResult fail = new GateResult(GateDecision.FAIL, 3, 1, NOW, List.of());

        listener.onGateEvaluated(new PipelineGateEvaluatedEvent(
                PV, "https://github.com/foo/bar", "42", fail));

        assertThat(provider.postedComments()).hasSize(1);
        FakeGitProvider.MergeRequestComment c = provider.postedComments().get(0);
        assertThat(c.mergeRequestId()).isEqualTo("42");
        assertThat(c.body()).contains("[FAIL]").contains("Neue CRITICAL: 3")
                .contains("Neue HIGH: 1");
    }

    @Test
    @DisplayName("disabled -> kein Post")
    void disabled() {
        PipelineGateMrCommentListener listener =
                new PipelineGateMrCommentListener(provider, false);
        listener.onGateEvaluated(new PipelineGateEvaluatedEvent(
                PV, "https://github.com/foo/bar", "42",
                new GateResult(GateDecision.PASS, 0, 0, NOW, List.of())));
        assertThat(provider.postedComments()).isEmpty();
    }

    @Test
    @DisplayName("ohne repoUrl oder mergeRequestId -> kein Post")
    void ohneKontext() {
        PipelineGateMrCommentListener listener =
                new PipelineGateMrCommentListener(provider, true);
        listener.onGateEvaluated(new PipelineGateEvaluatedEvent(
                PV, null, "42",
                new GateResult(GateDecision.PASS, 0, 0, NOW, List.of())));
        listener.onGateEvaluated(new PipelineGateEvaluatedEvent(
                PV, "https://github.com/foo/bar", null,
                new GateResult(GateDecision.PASS, 0, 0, NOW, List.of())));
        assertThat(provider.postedComments()).isEmpty();
    }

    @Test
    @DisplayName("Provider-Fehler wirft nicht nach aussen")
    void fehlerSchluckt() {
        provider.simulatePostFailure(true);
        PipelineGateMrCommentListener listener =
                new PipelineGateMrCommentListener(provider, true);
        // darf keine Exception werfen
        listener.onGateEvaluated(new PipelineGateEvaluatedEvent(
                PV, "https://github.com/foo/bar", "42",
                new GateResult(GateDecision.WARN, 0, 2, NOW, List.of())));
        assertThat(provider.postedComments()).isEmpty();
    }

    @Test
    @DisplayName("rendert PASS-Kommentar ohne Aktionsempfehlung")
    void passOhneAktion() {
        GateResult pass = new GateResult(GateDecision.PASS, 0, 0, NOW, List.of());
        String body = PipelineGateMrCommentListener.renderComment(pass);
        assertThat(body).contains("[PASS]").doesNotContain("Queue");
    }
}
