package com.ahs.cvm.application.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.application.alert.AlertEvaluator.AlertOutcome;
import com.ahs.cvm.application.assessment.AssessmentQueueService;
import com.ahs.cvm.application.assessment.AssessmentQueueService.QueueFilter;
import com.ahs.cvm.application.assessment.FindingQueueView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AlertTriggerArt;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AlertEscalationJobTest {

    private final Instant now = Instant.parse("2026-04-18T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final AlertConfig config = new AlertConfig("dry-run", 120, 360, "from@ahs.test");

    private FindingQueueView eintrag(AhsSeverity sev, Instant created) {
        return new FindingQueueView(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "CVE-2025-99",
                UUID.randomUUID(), UUID.randomUUID(),
                sev, AssessmentStatus.PROPOSED, ProposalSource.RULE,
                null, null, created, 1);
    }

    @Test
    @DisplayName("CRITICAL aelter als T2 -> ESKALATION_T2-Trigger")
    void t2Eskaliert() {
        AssessmentQueueService queue = mock(AssessmentQueueService.class);
        AlertEvaluator evaluator = mock(AlertEvaluator.class);
        given(queue.findeOffene(any(QueueFilter.class)))
                .willReturn(List.of(eintrag(AhsSeverity.CRITICAL, now.minusSeconds(7 * 3600))));
        given(evaluator.evaluate(any())).willReturn(new AlertOutcome(1, 0));
        AlertEscalationJob job = new AlertEscalationJob(queue, evaluator, config, clock);

        int n = job.runOnce();

        assertThat(n).isEqualTo(1);
        ArgumentCaptor<AlertContext> captor = ArgumentCaptor.forClass(AlertContext.class);
        verify(evaluator, times(1)).evaluate(captor.capture());
        assertThat(captor.getValue().triggerArt()).isEqualTo(AlertTriggerArt.ESKALATION_T2);
    }

    @Test
    @DisplayName("CRITICAL zwischen T1 und T2 -> ESKALATION_T1-Trigger")
    void t1Eskaliert() {
        AssessmentQueueService queue = mock(AssessmentQueueService.class);
        AlertEvaluator evaluator = mock(AlertEvaluator.class);
        given(queue.findeOffene(any(QueueFilter.class)))
                .willReturn(List.of(eintrag(AhsSeverity.CRITICAL, now.minusSeconds(3 * 3600))));
        given(evaluator.evaluate(any())).willReturn(new AlertOutcome(1, 0));
        AlertEscalationJob job = new AlertEscalationJob(queue, evaluator, config, clock);

        job.runOnce();

        ArgumentCaptor<AlertContext> captor = ArgumentCaptor.forClass(AlertContext.class);
        verify(evaluator).evaluate(captor.capture());
        assertThat(captor.getValue().triggerArt()).isEqualTo(AlertTriggerArt.ESKALATION_T1);
    }

    @Test
    @DisplayName("HIGH wird ignoriert, kein Trigger ausgeloest")
    void hochAberNichtCritical_ignoriert() {
        AssessmentQueueService queue = mock(AssessmentQueueService.class);
        AlertEvaluator evaluator = mock(AlertEvaluator.class);
        given(queue.findeOffene(any(QueueFilter.class)))
                .willReturn(List.of(eintrag(AhsSeverity.HIGH, now.minusSeconds(8 * 3600))));
        AlertEscalationJob job = new AlertEscalationJob(queue, evaluator, config, clock);

        int n = job.runOnce();

        assertThat(n).isZero();
        verify(evaluator, times(0)).evaluate(any());
    }
}
