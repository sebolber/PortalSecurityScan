package com.ahs.cvm.application.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.assessment.AssessmentQueueService;
import com.ahs.cvm.application.assessment.AssessmentQueueService.QueueFilter;
import com.ahs.cvm.application.assessment.FindingQueueView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlertBannerServiceTest {

    private final Instant now = Instant.parse("2026-04-18T13:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final AlertConfig config = new AlertConfig("dry-run", 120, 360, "from@ahs.test");

    @Test
    @DisplayName("Banner aktiv, sobald CRITICAL aelter als T2")
    void bannerAktivBeiUeberfaelligemCritical() {
        AssessmentQueueService queue = mock(AssessmentQueueService.class);
        given(queue.findeOffene(any(QueueFilter.class)))
                .willReturn(List.of(view(AhsSeverity.CRITICAL, now.minusSeconds(7 * 3600))));
        AlertBannerService service = new AlertBannerService(queue, config, clock);

        var status = service.aktuellerStatus();

        assertThat(status.visible()).isTrue();
        assertThat(status.count()).isEqualTo(1);
        assertThat(status.t2Minutes()).isEqualTo(360);
    }

    @Test
    @DisplayName("Banner inaktiv, wenn alle CRITICAL juenger als T2")
    void bannerInaktivOhneUeberfaelligkeit() {
        AssessmentQueueService queue = mock(AssessmentQueueService.class);
        given(queue.findeOffene(any(QueueFilter.class)))
                .willReturn(List.of(view(AhsSeverity.CRITICAL, now.minusSeconds(60 * 60))));
        AlertBannerService service = new AlertBannerService(queue, config, clock);

        assertThat(service.aktuellerStatus().visible()).isFalse();
    }

    private FindingQueueView view(AhsSeverity sev, Instant created) {
        return new FindingQueueView(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "CVE-X", UUID.randomUUID(), UUID.randomUUID(),
                sev, AssessmentStatus.PROPOSED, ProposalSource.RULE,
                null, null, created, 1);
    }
}
