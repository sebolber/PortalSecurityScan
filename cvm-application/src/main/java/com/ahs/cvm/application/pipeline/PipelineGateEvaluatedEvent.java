package com.ahs.cvm.application.pipeline;

import com.ahs.cvm.application.pipeline.PipelineGateService.GateResult;
import java.util.UUID;

/**
 * Wird vom {@link PipelineGateService} nach jeder Gate-Auswertung
 * publiziert (Iteration 22, CVM-53).
 *
 * <p>Listener (z.&nbsp;B. im Integration-Modul) koennen daraufhin
 * Folgeaktionen wie einen MR-Kommentar im Git-Provider ausloesen.
 * Die Publikation ist synchron; Listener muessen selbst entscheiden,
 * ob sie Async arbeiten wollen.
 *
 * @param productVersionId Kontext-Produktversion.
 * @param repoUrl Upstream-Repo-URL ({@code null} moeglich).
 * @param mergeRequestId Provider-spezifischer MR/PR-Identifier
 *     ({@code null} moeglich).
 * @param result Gate-Ergebnis (PASS/WARN/FAIL inkl. Zaehler).
 */
public record PipelineGateEvaluatedEvent(
        UUID productVersionId,
        String repoUrl,
        String mergeRequestId,
        GateResult result) {}
