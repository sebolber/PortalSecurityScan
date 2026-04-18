package com.ahs.cvm.application.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.MitigationStatus;
import com.ahs.cvm.domain.enums.MitigationStrategy;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.mitigation.MitigationPlan;
import com.ahs.cvm.persistence.mitigation.MitigationPlanRepository;
import com.ahs.cvm.persistence.product.ProductVersion;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class AssessmentWriteServiceTest {

    private AssessmentRepository assessmentRepository;
    private FindingRepository findingRepository;
    private MitigationPlanRepository mitigationRepository;
    private ApplicationEventPublisher eventPublisher;
    private AssessmentWriteService service;

    private final UUID findingId = UUID.randomUUID();
    private final UUID productVersionId = UUID.randomUUID();
    private final UUID environmentId = UUID.randomUUID();
    private final UUID cveId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        findingRepository = mock(FindingRepository.class);
        mitigationRepository = mock(MitigationPlanRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        AssessmentStateMachine stateMachine = new AssessmentStateMachine();
        AssessmentConfig config = new AssessmentConfig(12);
        service = new AssessmentWriteService(
                assessmentRepository,
                findingRepository,
                mitigationRepository,
                stateMachine,
                eventPublisher,
                config);
        given(assessmentRepository.save(any(Assessment.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(findingRepository.findById(findingId)).willReturn(Optional.of(bestehendesFinding()));
    }

    @Test
    @DisplayName("Cascade-Propose: RULE-Outcome -> PROPOSED-Assessment wird persistiert")
    void cascadeProposeRule() {
        given(assessmentRepository.findFirstByFindingIdOrderByVersionDesc(findingId))
                .willReturn(Optional.empty());

        Assessment result = service.propose(new AssessmentWriteService.ProposeCommand(
                findingId,
                cveId,
                productVersionId,
                environmentId,
                ProposalSource.RULE,
                AhsSeverity.LOW,
                "keine Windows-Plattform im Einsatz",
                List.of("profile.architecture.windows_hosts"),
                UUID.randomUUID(),
                null));

        assertThat(result.getStatus()).isEqualTo(AssessmentStatus.PROPOSED);
        assertThat(result.getProposalSource()).isEqualTo(ProposalSource.RULE);
        assertThat(result.getVersion()).isEqualTo(1);
        verify(assessmentRepository, times(1)).save(any(Assessment.class));
    }

    @Test
    @DisplayName("Cascade-Propose: REUSE-Outcome -> APPROVED-Fortschreibung mit validUntil")
    void cascadeProposeReuse() {
        Assessment vorhanden = Assessment.builder()
                .id(UUID.randomUUID())
                .version(1)
                .status(AssessmentStatus.APPROVED)
                .severity(AhsSeverity.MEDIUM)
                .proposalSource(ProposalSource.HUMAN)
                .rationale("bereits bewertet")
                .build();
        given(assessmentRepository.findFirstByFindingIdOrderByVersionDesc(findingId))
                .willReturn(Optional.of(vorhanden));

        Instant now = Instant.now();
        Assessment result = service.propose(new AssessmentWriteService.ProposeCommand(
                findingId,
                cveId,
                productVersionId,
                environmentId,
                ProposalSource.REUSE,
                AhsSeverity.MEDIUM,
                "bereits bewertet",
                List.of(),
                null,
                vorhanden.getId()));

        assertThat(result.getStatus()).isEqualTo(AssessmentStatus.APPROVED);
        assertThat(result.getProposalSource()).isEqualTo(ProposalSource.REUSE);
        assertThat(result.getVersion()).isEqualTo(2);
        assertThat(result.getValidUntil()).isAfter(now);
    }

    @Test
    @DisplayName("Cascade-Propose: HUMAN-Outcome -> nichts wird persistiert")
    void cascadeProposeHumanOhneSchreiben() {
        Assessment result = service.propose(new AssessmentWriteService.ProposeCommand(
                findingId,
                cveId,
                productVersionId,
                environmentId,
                ProposalSource.HUMAN,
                null,
                null,
                List.of(),
                null,
                null));

        assertThat(result).isNull();
        verify(assessmentRepository, times(0)).save(any(Assessment.class));
    }

    @Test
    @DisplayName("manualPropose: Autor legt eigenen PROPOSED-Eintrag an")
    void manualProposeAutor() {
        given(assessmentRepository.findFirstByFindingIdOrderByVersionDesc(findingId))
                .willReturn(Optional.empty());

        Assessment result = service.manualPropose(new AssessmentWriteService.ManualProposeCommand(
                findingId,
                AhsSeverity.HIGH,
                "Produkt nutzt verwundbares Modul",
                List.of(),
                "t.tester@ahs.test",
                productVersionId,
                environmentId));

        assertThat(result.getStatus()).isEqualTo(AssessmentStatus.PROPOSED);
        assertThat(result.getProposalSource()).isEqualTo(ProposalSource.HUMAN);
        assertThat(result.getDecidedBy()).isEqualTo("t.tester@ahs.test");
        assertThat(result.getSeverity()).isEqualTo(AhsSeverity.HIGH);
    }

    @Test
    @DisplayName("approve: Nicht-Downgrade erlaubt Autor=Approver (Single-Step)")
    void approveSingleStepErlaubt() {
        Assessment proposed = proposedAssessment(AhsSeverity.HIGH, "t.tester@ahs.test");
        given(assessmentRepository.findById(proposed.getId())).willReturn(Optional.of(proposed));

        Assessment result = service.approve(proposed.getId(), "t.tester@ahs.test");

        assertThat(result.getStatus()).isEqualTo(AssessmentStatus.APPROVED);
        assertThat(result.getVersion()).isEqualTo(proposed.getVersion() + 1);
        assertThat(result.getDecidedBy()).isEqualTo("t.tester@ahs.test");
        verify(eventPublisher, times(1)).publishEvent(any(AssessmentApprovedEvent.class));
    }

    @Test
    @DisplayName("Vier-Augen: Downgrade auf NOT_APPLICABLE durch Autor schlaegt fehl")
    void vierAugenDowngradeAutorAblehnen() {
        Assessment proposed = proposedAssessment(AhsSeverity.NOT_APPLICABLE, "t.tester@ahs.test");
        given(assessmentRepository.findById(proposed.getId())).willReturn(Optional.of(proposed));

        assertThatThrownBy(() -> service.approve(proposed.getId(), "t.tester@ahs.test"))
                .isInstanceOf(AssessmentFourEyesViolationException.class);
    }

    @Test
    @DisplayName("Vier-Augen: Downgrade auf INFORMATIONAL durch anderen Approver freigegeben")
    void vierAugenDowngradeZweitfreigabe() {
        Assessment proposed = proposedAssessment(AhsSeverity.INFORMATIONAL, "t.tester@ahs.test");
        given(assessmentRepository.findById(proposed.getId())).willReturn(Optional.of(proposed));

        Assessment result = service.approve(proposed.getId(), "a.admin@ahs.test");

        assertThat(result.getStatus()).isEqualTo(AssessmentStatus.APPROVED);
        assertThat(result.getReviewedBy()).isEqualTo("a.admin@ahs.test");
    }

    @Test
    @DisplayName("reject: neue Version REJECTED, Kommentar pflicht")
    void rejectKommentarPflicht() {
        Assessment proposed = proposedAssessment(AhsSeverity.HIGH, "t.tester@ahs.test");
        given(assessmentRepository.findById(proposed.getId())).willReturn(Optional.of(proposed));

        assertThatThrownBy(() -> service.reject(proposed.getId(), "a.admin@ahs.test", "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("reject: erzeugt neue Version REJECTED und supersediert PROPOSED")
    void rejectErfolg() {
        Assessment proposed = proposedAssessment(AhsSeverity.HIGH, "t.tester@ahs.test");
        given(assessmentRepository.findById(proposed.getId())).willReturn(Optional.of(proposed));

        Assessment result = service.reject(proposed.getId(), "a.admin@ahs.test",
                "Befund ist ein False-Positive");

        assertThat(result.getStatus()).isEqualTo(AssessmentStatus.REJECTED);
        assertThat(result.getRationale()).contains("False-Positive");
        assertThat(proposed.getSupersededAt()).isNotNull();
    }

    @Test
    @DisplayName("expireIfDue: publiziert AssessmentExpiredEvent mit allen Ids")
    void expireIfDuePubliziertEvent() {
        UUID a1 = UUID.randomUUID();
        UUID a2 = UUID.randomUUID();
        Assessment ex1 = Assessment.builder().id(a1).version(1)
                .finding(bestehendesFinding())
                .productVersion(ProductVersion.builder().id(productVersionId).build())
                .environment(Environment.builder().id(environmentId).build())
                .cve(Cve.builder().id(cveId).build())
                .status(AssessmentStatus.APPROVED).severity(AhsSeverity.HIGH)
                .proposalSource(ProposalSource.HUMAN).build();
        Assessment ex2 = Assessment.builder().id(a2).version(1)
                .finding(bestehendesFinding())
                .productVersion(ProductVersion.builder().id(productVersionId).build())
                .environment(Environment.builder().id(environmentId).build())
                .cve(Cve.builder().id(cveId).build())
                .status(AssessmentStatus.APPROVED).severity(AhsSeverity.MEDIUM)
                .proposalSource(ProposalSource.HUMAN).build();
        given(assessmentRepository
                .findByStatusAndValidUntilLessThanEqualAndSupersededAtIsNull(
                        AssessmentStatus.APPROVED, Instant.parse("2026-05-01T00:00:00Z")))
                .willReturn(List.of(ex1, ex2));
        given(assessmentRepository.markiereAlsAbgelaufen(any(), any())).willReturn(2);

        int betroffen = service.expireIfDue(Instant.parse("2026-05-01T00:00:00Z"));

        assertThat(betroffen).isEqualTo(2);
        ArgumentCaptor<AssessmentExpiredEvent> cap =
                ArgumentCaptor.forClass(AssessmentExpiredEvent.class);
        verify(eventPublisher).publishEvent(cap.capture());
        assertThat(cap.getValue().assessmentIds()).containsExactly(a1, a2);
    }

    @Test
    @DisplayName("expireIfDue: keine Kandidaten -> kein Event")
    void expireIfDueLeer() {
        given(assessmentRepository
                .findByStatusAndValidUntilLessThanEqualAndSupersededAtIsNull(
                        any(), any()))
                .willReturn(List.of());

        int betroffen = service.expireIfDue(Instant.parse("2026-05-01T00:00:00Z"));

        assertThat(betroffen).isZero();
        verify(eventPublisher, times(0)).publishEvent(any(AssessmentExpiredEvent.class));
    }

    @Test
    @DisplayName("approve: mit Mitigation-Plan persistiert Strategie und Zieltermin")
    void approveMitMitigationPlan() {
        Assessment proposed = proposedAssessment(AhsSeverity.HIGH, "t.tester@ahs.test");
        given(assessmentRepository.findById(proposed.getId())).willReturn(Optional.of(proposed));
        given(mitigationRepository.save(any(MitigationPlan.class)))
                .willAnswer(inv -> inv.getArgument(0));
        Instant plan = Instant.now().plus(14, ChronoUnit.DAYS);

        service.approveMitMitigation(
                proposed.getId(),
                "a.admin@ahs.test",
                new AssessmentWriteService.MitigationInput(
                        MitigationStrategy.UPGRADE, "1.14.3-test", plan, "Upgrade geplant"));

        ArgumentCaptor<MitigationPlan> captor = ArgumentCaptor.forClass(MitigationPlan.class);
        verify(mitigationRepository).save(captor.capture());
        MitigationPlan persistiert = captor.getValue();
        assertThat(persistiert.getStrategy()).isEqualTo(MitigationStrategy.UPGRADE);
        assertThat(persistiert.getStatus()).isEqualTo(MitigationStatus.PLANNED);
        assertThat(persistiert.getPlannedFor()).isEqualTo(plan);
    }

    private Assessment proposedAssessment(AhsSeverity severity, String decidedBy) {
        return Assessment.builder()
                .id(UUID.randomUUID())
                .finding(bestehendesFinding())
                .productVersion(ProductVersion.builder().id(productVersionId).build())
                .environment(Environment.builder().id(environmentId).build())
                .cve(Cve.builder().id(cveId).build())
                .version(1)
                .status(AssessmentStatus.PROPOSED)
                .severity(severity)
                .proposalSource(ProposalSource.HUMAN)
                .decidedBy(decidedBy)
                .rationale("Vorschlag")
                .build();
    }

    private Finding bestehendesFinding() {
        return Finding.builder()
                .id(findingId)
                .cve(Cve.builder().id(cveId).build())
                .build();
    }
}
