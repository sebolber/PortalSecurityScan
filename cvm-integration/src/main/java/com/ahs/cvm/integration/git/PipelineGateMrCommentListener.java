package com.ahs.cvm.integration.git;

import com.ahs.cvm.application.pipeline.PipelineGateEvaluatedEvent;
import com.ahs.cvm.application.pipeline.PipelineGateService.GateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Postet das Gate-Ergebnis als Markdown-Kommentar an den MR/PR
 * (Iteration 22, CVM-53).
 *
 * <p>Aktiv nur, wenn:
 * <ul>
 *   <li>das Feature-Flag {@code cvm.pipeline.gate.post-mr-comment=true} ist,</li>
 *   <li>das Event {@code repoUrl} UND {@code mergeRequestId} traegt,</li>
 *   <li>der aktive {@code GitProviderPort} den Post akzeptiert.</li>
 * </ul>
 *
 * <p>Fehler im Provider (Netzwerk, 403, fehlender Token) bleiben
 * Warnungen im Log; das Gate-Ergebnis wurde ja bereits geliefert.
 */
@Component
public class PipelineGateMrCommentListener {

    private static final Logger log = LoggerFactory.getLogger(
            PipelineGateMrCommentListener.class);

    private final GitProviderPort gitProvider;
    private final boolean enabled;

    public PipelineGateMrCommentListener(
            GitProviderPort gitProvider,
            @Value("${cvm.pipeline.gate.post-mr-comment:false}") boolean enabled) {
        this.gitProvider = gitProvider;
        this.enabled = enabled;
    }

    @EventListener
    public void onGateEvaluated(PipelineGateEvaluatedEvent event) {
        if (!enabled) {
            return;
        }
        if (event.repoUrl() == null || event.repoUrl().isBlank()
                || event.mergeRequestId() == null || event.mergeRequestId().isBlank()) {
            log.debug("Gate-Event ohne repoUrl/MR-Id, kein Kommentar.");
            return;
        }
        String body = renderComment(event.result());
        try {
            boolean ok = gitProvider.postMergeRequestComment(
                    event.repoUrl(), event.mergeRequestId(), body);
            if (!ok) {
                log.warn("Gate-Kommentar an {}#{} wurde nicht akzeptiert.",
                        event.repoUrl(), event.mergeRequestId());
            }
        } catch (RuntimeException ex) {
            log.warn("Gate-Kommentar an {}#{} fehlgeschlagen: {}",
                    event.repoUrl(), event.mergeRequestId(), ex.getMessage());
        }
    }

    static String renderComment(GateResult result) {
        String icon = switch (result.gate()) {
            case PASS -> "[PASS]";
            case WARN -> "[WARN]";
            case FAIL -> "[FAIL]";
        };
        StringBuilder sb = new StringBuilder();
        sb.append("**CVE-Relevance-Manager Gate: ").append(icon).append("**\n\n");
        sb.append("- Neue CRITICAL: ").append(result.newCritical()).append('\n');
        sb.append("- Neue HIGH: ").append(result.newHigh()).append('\n');
        sb.append("- Geprueft um: ").append(result.evaluatedAt()).append('\n');
        if (result.gate() != com.ahs.cvm.application.pipeline.PipelineGateService
                .GateDecision.PASS) {
            sb.append('\n').append("Bitte Assessments in der CVM-Queue bearbeiten.");
        }
        return sb.toString();
    }
}
