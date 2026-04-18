package com.ahs.cvm.application.alert;

import com.ahs.cvm.application.assessment.AssessmentApprovedEvent;
import com.ahs.cvm.application.scan.ScanIngestedEvent;
import com.ahs.cvm.domain.enums.AlertTriggerArt;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Verbindet bestehende Domain-Events mit dem {@link AlertEvaluator}.
 *
 * <p>Iteration 09 deckt zwei Quellen ab:
 * <ul>
 *   <li>{@link AssessmentApprovedEvent} -> {@code ASSESSMENT_APPROVED}.</li>
 *   <li>{@link ScanIngestedEvent} -> {@code FINDING_NEU} pro Scan-Run.
 *       Pro einzelnem Finding wird kein Alert ausgeloest, sondern eine
 *       gesammelte Notiz pro Scan; einzelne Findings koennen ueber
 *       Cascade ohnehin den ASSESSMENT_PROPOSED-Pfad triggern, wenn
 *       der Workflow das mal verdrahtet (siehe offene-punkte.md).</li>
 * </ul>
 */
@Component
public class AlertEventListeners {

    private final AlertEvaluator evaluator;
    private final Clock clock;

    public AlertEventListeners(AlertEvaluator evaluator, Clock clock) {
        this.evaluator = evaluator;
        this.clock = clock;
    }

    @EventListener
    public void onApproved(AssessmentApprovedEvent event) {
        AlertContext ctx = new AlertContext(
                AlertTriggerArt.ASSESSMENT_APPROVED,
                "assessment|" + event.assessmentId(),
                event.severity(),
                event.cveId(),
                event.assessmentId(),
                event.productVersionId(),
                event.environmentId(),
                "Assessment freigegeben durch " + event.approverId(),
                event.approvedAt() == null ? Instant.now(clock) : event.approvedAt(),
                Map.of(
                        "approver", String.valueOf(event.approverId()),
                        "umgebung", String.valueOf(event.environmentId()),
                        "produktVersion", String.valueOf(event.productVersionId())));
        evaluator.evaluate(ctx);
    }

    @EventListener
    public void onScanIngested(ScanIngestedEvent event) {
        AlertContext ctx = new AlertContext(
                AlertTriggerArt.FINDING_NEU,
                "scan|" + event.scanId(),
                null,
                null,
                null,
                event.productVersionId(),
                event.environmentId(),
                "Neuer Scan ingested mit " + event.findingCount() + " Findings",
                event.ingestedAt() == null ? Instant.now(clock) : event.ingestedAt(),
                Map.of(
                        "scanId", String.valueOf(event.scanId()),
                        "umgebung", String.valueOf(event.environmentId()),
                        "produktVersion", String.valueOf(event.productVersionId()),
                        "findingCount", String.valueOf(event.findingCount())));
        evaluator.evaluate(ctx);
    }
}
