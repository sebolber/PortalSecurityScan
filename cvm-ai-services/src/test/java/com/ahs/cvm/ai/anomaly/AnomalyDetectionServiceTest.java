package com.ahs.cvm.ai.anomaly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.MitigationStatus;
import com.ahs.cvm.domain.enums.MitigationStrategy;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.persistence.anomaly.AnomalyEvent;
import com.ahs.cvm.persistence.anomaly.AnomalyEventRepository;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.mitigation.MitigationPlan;
import com.ahs.cvm.persistence.mitigation.MitigationPlanRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnomalyDetectionServiceTest {

    private AssessmentRepository assessmentRepo;
    private MitigationPlanRepository mitigationRepo;
    private AnomalyEventRepository anomalyRepo;
    private AnomalyDetectionService service;

    @BeforeEach
    void setUp() {
        assessmentRepo = mock(AssessmentRepository.class);
        mitigationRepo = mock(MitigationPlanRepository.class);
        anomalyRepo = mock(AnomalyEventRepository.class);
        given(anomalyRepo.save(any(AnomalyEvent.class))).willAnswer(inv -> inv.getArgument(0));
        given(anomalyRepo.existsByAssessmentIdAndPattern(any(), any())).willReturn(false);
        given(mitigationRepo.findByAssessmentId(any())).willReturn(List.of());
        service = new AnomalyDetectionService(
                new AnomalyConfig(true, 0.7, 5, 0.9, false),
                assessmentRepo, mitigationRepo, anomalyRepo);
    }

    private Cve cve(String key, boolean kev, double epss) {
        return Cve.builder().id(UUID.randomUUID()).cveId(key)
                .kevListed(kev).epssScore(BigDecimal.valueOf(epss)).build();
    }

    private Assessment assessment(AhsSeverity severity, Cve cve, Instant when) {
        return Assessment.builder()
                .id(UUID.randomUUID())
                .cve(cve)
                .severity(severity)
                .status(AssessmentStatus.APPROVED)
                .proposalSource(ProposalSource.HUMAN)
                .decidedBy("a.admin@ahs.test")
                .decidedAt(when)
                .createdAt(when)
                .finding(Finding.builder().id(UUID.randomUUID()).build())
                .version(1)
                .build();
    }

    @Test
    @DisplayName("Anomalie-Check: NOT_APPLICABLE trotz KEV und hohem EPSS loest WARNING aus")
    void kevNotApplicable() {
        Assessment a = assessment(AhsSeverity.NOT_APPLICABLE,
                cve("CVE-X", true, 0.9), Instant.now());
        given(assessmentRepo.findAll()).willReturn(List.of(a));

        List<AnomalyEvent> events = service.check(Instant.now().minus(Duration.ofHours(24)));

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getPattern()).isEqualTo("KEV_NOT_APPLICABLE");
        assertThat(events.get(0).getSeverity()).isEqualTo("WARNING");
        // Invariante: Service schreibt kein Assessment.
        verify(assessmentRepo, never()).save(any(Assessment.class));
    }

    @Test
    @DisplayName("Anomalie-Check: KEV mit niedrigem EPSS loest KEIN Event aus")
    void kevNiedrigesEpss() {
        Assessment a = assessment(AhsSeverity.NOT_APPLICABLE,
                cve("CVE-X", true, 0.3), Instant.now());
        given(assessmentRepo.findAll()).willReturn(List.of(a));

        List<AnomalyEvent> events = service.check(Instant.now().minus(Duration.ofHours(24)));

        assertThat(events).isEmpty();
    }

    @Test
    @DisplayName("Anomalie-Check: >=5 ACCEPT_RISK-Waiver eines Bewerters erzeugt MANY_ACCEPT_RISK")
    void manyWaiver() {
        List<Assessment> alle = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Assessment a = assessment(AhsSeverity.MEDIUM,
                    cve("CVE-" + i, false, 0.1), Instant.now());
            alle.add(a);
            given(mitigationRepo.findByAssessmentId(a.getId()))
                    .willReturn(List.of(MitigationPlan.builder()
                            .id(UUID.randomUUID())
                            .strategy(MitigationStrategy.ACCEPT_RISK)
                            .status(MitigationStatus.PLANNED).build()));
        }
        given(assessmentRepo.findAll()).willReturn(alle);

        List<AnomalyEvent> events = service.check(Instant.now().minus(Duration.ofHours(24)));

        assertThat(events).isNotEmpty();
        assertThat(events).anyMatch(e -> "MANY_ACCEPT_RISK".equals(e.getPattern()));
    }

    @Test
    @DisplayName("Anomalie-Check: aehnliches Rationale zu abgelehntem Vorschlag erzeugt INFO")
    void similarToRejected() {
        Assessment neu = assessment(AhsSeverity.MEDIUM,
                cve("CVE-N", false, 0.1), Instant.now());
        neu.setRationale("Kein Nutzer-Input im Pfad");
        Assessment rejected = Assessment.builder()
                .id(UUID.randomUUID())
                .cve(cve("CVE-R", false, 0.0))
                .severity(AhsSeverity.MEDIUM)
                .status(AssessmentStatus.REJECTED)
                .proposalSource(ProposalSource.HUMAN)
                .rationale("Kein Nutzer Input im Pfad")
                .version(1)
                .build();
        given(assessmentRepo.findAll()).willReturn(List.of(neu, rejected));

        List<AnomalyEvent> events = service.check(Instant.now().minus(Duration.ofHours(24)));

        assertThat(events).anyMatch(e -> "SIMILAR_TO_REJECTED".equals(e.getPattern()));
    }

    @Test
    @DisplayName("Anomalie-Check: Downgrade um zwei Severity-Stufen ohne RULE-Herkunft erzeugt CRITICAL")
    void bigDowngrade() {
        UUID findingId = UUID.randomUUID();
        Assessment v1 = Assessment.builder()
                .id(UUID.randomUUID())
                .finding(Finding.builder().id(findingId).build())
                .cve(cve("CVE-X", false, 0.1))
                .severity(AhsSeverity.HIGH)
                .status(AssessmentStatus.SUPERSEDED)
                .proposalSource(ProposalSource.HUMAN)
                .version(1)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        Assessment v2 = Assessment.builder()
                .id(UUID.randomUUID())
                .finding(Finding.builder().id(findingId).build())
                .cve(cve("CVE-X", false, 0.1))
                .severity(AhsSeverity.LOW)
                .status(AssessmentStatus.APPROVED)
                .proposalSource(ProposalSource.HUMAN)
                .version(2)
                .createdAt(Instant.now())
                .build();
        given(assessmentRepo.findAll()).willReturn(List.of(v1, v2));

        List<AnomalyEvent> events = service.check(Instant.now().minus(Duration.ofHours(24)));

        assertThat(events).anyMatch(e -> "BIG_DOWNGRADE_WITHOUT_RULE".equals(e.getPattern()));
    }

    @Test
    @DisplayName("Anomalie-Check: Flag aus -> keine Pruefung, keine Events")
    void flagAus() {
        service = new AnomalyDetectionService(
                new AnomalyConfig(false, 0.7, 5, 0.9, false),
                assessmentRepo, mitigationRepo, anomalyRepo);
        given(assessmentRepo.findAll()).willReturn(List.of(
                assessment(AhsSeverity.NOT_APPLICABLE, cve("CVE-X", true, 0.95), Instant.now())));

        assertThat(service.check(Instant.now())).isEmpty();
    }

    @Test
    @DisplayName("Anomalie-Check: duplikat-Event wird nicht zweimal gespeichert")
    void dedup() {
        Assessment a = assessment(AhsSeverity.NOT_APPLICABLE,
                cve("CVE-X", true, 0.9), Instant.now());
        given(assessmentRepo.findAll()).willReturn(List.of(a));
        given(anomalyRepo.existsByAssessmentIdAndPattern(a.getId(), "KEV_NOT_APPLICABLE"))
                .willReturn(true);

        service.check(Instant.now().minus(Duration.ofHours(24)));

        verify(anomalyRepo, never()).save(any(AnomalyEvent.class));
    }
}
