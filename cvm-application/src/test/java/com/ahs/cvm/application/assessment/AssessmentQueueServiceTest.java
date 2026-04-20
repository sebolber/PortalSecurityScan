package com.ahs.cvm.application.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.product.ProductVersion;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssessmentQueueServiceTest {

    private final AssessmentRepository repository = mock(AssessmentRepository.class);
    private final AssessmentQueueService service = new AssessmentQueueService(repository);

    @Test
    @DisplayName("findeOffene filtert nach environmentId und liefert View-Liste")
    void filterNachEnv() {
        UUID envId = UUID.randomUUID();
        given(repository.findeQueueNachStatus(isNull(), eq(envId), any(), any()))
                .willReturn(List.of(
                        eintrag(AhsSeverity.HIGH, AssessmentStatus.PROPOSED),
                        eintrag(AhsSeverity.LOW, AssessmentStatus.NEEDS_REVIEW)));

        List<FindingQueueView> offen = service.findeOffene(
                new AssessmentQueueService.QueueFilter(null, envId, null, null));

        assertThat(offen).hasSize(2);
        assertThat(offen).extracting(FindingQueueView::severity)
                .containsExactly(AhsSeverity.HIGH, AhsSeverity.LOW);
    }

    @Test
    @DisplayName("findeOffene mit Status-Filter PROPOSED leitet den Status ans Repository durch")
    void statusFilter() {
        given(repository.findeQueueNachStatus(eq(AssessmentStatus.PROPOSED), any(), any(), any()))
                .willReturn(List.of(eintrag(AhsSeverity.HIGH, AssessmentStatus.PROPOSED)));

        List<FindingQueueView> offen = service.findeOffene(
                new AssessmentQueueService.QueueFilter(
                        AssessmentStatus.PROPOSED, null, null, null));

        assertThat(offen).hasSize(1);
        assertThat(offen.get(0).status()).isEqualTo(AssessmentStatus.PROPOSED);
        verify(repository).findeQueueNachStatus(
                eq(AssessmentStatus.PROPOSED), any(), any(), any());
    }

    @Test
    @DisplayName("Iteration 99: Status=APPROVED liefert die vom Repository zurueckgegebenen Zeilen")
    void approvedFilter() {
        given(repository.findeQueueNachStatus(
                eq(AssessmentStatus.APPROVED), any(), any(), any()))
                .willReturn(List.of(eintrag(AhsSeverity.HIGH, AssessmentStatus.APPROVED)));

        List<FindingQueueView> offen = service.findeOffene(
                new AssessmentQueueService.QueueFilter(
                        AssessmentStatus.APPROVED, null, null, null));

        assertThat(offen).hasSize(1);
        assertThat(offen.get(0).status()).isEqualTo(AssessmentStatus.APPROVED);
    }

    @Test
    @DisplayName("Iteration 99: Status=null liefert alles aus dem Repository (ALLE-Chip)")
    void statusNullReichtDurch() {
        given(repository.findeQueueNachStatus(isNull(), any(), any(), any()))
                .willReturn(List.of(
                        eintrag(AhsSeverity.HIGH, AssessmentStatus.PROPOSED),
                        eintrag(AhsSeverity.MEDIUM, AssessmentStatus.APPROVED),
                        eintrag(AhsSeverity.LOW, AssessmentStatus.REJECTED)));

        List<FindingQueueView> offen = service.findeOffene(
                new AssessmentQueueService.QueueFilter(null, null, null, null));

        assertThat(offen).hasSize(3);
        assertThat(offen).extracting(FindingQueueView::status)
                .containsExactly(
                        AssessmentStatus.PROPOSED,
                        AssessmentStatus.APPROVED,
                        AssessmentStatus.REJECTED);
    }

    private Assessment eintrag(AhsSeverity severity, AssessmentStatus status) {
        return Assessment.builder()
                .id(UUID.randomUUID())
                .finding(Finding.builder().id(UUID.randomUUID())
                        .cve(Cve.builder().id(UUID.randomUUID()).cveId("CVE-X").build())
                        .build())
                .productVersion(ProductVersion.builder().id(UUID.randomUUID()).build())
                .environment(Environment.builder().id(UUID.randomUUID()).build())
                .cve(Cve.builder().id(UUID.randomUUID()).cveId("CVE-X").build())
                .severity(severity)
                .status(status)
                .proposalSource(ProposalSource.RULE)
                .version(1)
                .build();
    }
}
