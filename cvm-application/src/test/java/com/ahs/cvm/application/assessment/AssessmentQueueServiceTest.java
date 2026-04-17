package com.ahs.cvm.application.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
        given(repository.findeOffeneQueue(eq(envId), any(), any()))
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
    @DisplayName("findeOffene mit Status-Filter PROPOSED blendet NEEDS_REVIEW aus")
    void statusFilter() {
        given(repository.findeOffeneQueue(any(), any(), any()))
                .willReturn(List.of(
                        eintrag(AhsSeverity.HIGH, AssessmentStatus.PROPOSED),
                        eintrag(AhsSeverity.LOW, AssessmentStatus.NEEDS_REVIEW)));

        List<FindingQueueView> offen = service.findeOffene(
                new AssessmentQueueService.QueueFilter(
                        AssessmentStatus.PROPOSED, null, null, null));

        assertThat(offen).hasSize(1);
        assertThat(offen.get(0).status()).isEqualTo(AssessmentStatus.PROPOSED);
    }

    @Test
    @DisplayName("findeOffene liefert leere Liste fuer Status ausserhalb PROPOSED/NEEDS_REVIEW")
    void unzulaessigerStatus() {
        assertThat(service.findeOffene(
                new AssessmentQueueService.QueueFilter(
                        AssessmentStatus.APPROVED, null, null, null)))
                .isEmpty();
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
