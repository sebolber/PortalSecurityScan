package com.ahs.cvm.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DashboardKpiServiceTest {

    private static final Instant JETZT =
            Instant.parse("2026-04-20T10:00:00Z");

    private final AssessmentRepository repo = mock(AssessmentRepository.class);
    private final Clock clock = Clock.fixed(JETZT, ZoneOffset.UTC);
    private final DashboardKpiService service = new DashboardKpiService(repo, clock);

    @Test
    @DisplayName("berechne liefert 0/leer, wenn keine offenen Zeilen existieren")
    void leer() {
        given(repo.findeQueueNachStatus(any(), any(), any(), any()))
                .willReturn(List.of());

        DashboardKpiView view = service.berechne();

        assertThat(view.offeneCves()).isZero();
        assertThat(view.aeltesteKritisch()).isNull();
        assertThat(view.weiterbetriebOk()).isTrue();
        assertThat(view.severityVerteilung().get(AhsSeverity.CRITICAL)).isZero();
    }

    @Test
    @DisplayName("berechne zaehlt nur offene Statusse und gruppiert nach Severity")
    void zaehltOffeneProSeverity() {
        given(repo.findeQueueNachStatus(any(), any(), any(), any()))
                .willReturn(List.of(
                        eintrag(AhsSeverity.CRITICAL, AssessmentStatus.PROPOSED, JETZT.minusSeconds(3_600)),
                        eintrag(AhsSeverity.HIGH, AssessmentStatus.NEEDS_REVIEW, JETZT),
                        eintrag(AhsSeverity.LOW, AssessmentStatus.APPROVED, JETZT)));

        DashboardKpiView view = service.berechne();

        assertThat(view.offeneCves()).isEqualTo(2);
        assertThat(view.severityVerteilung().get(AhsSeverity.CRITICAL)).isEqualTo(1);
        assertThat(view.severityVerteilung().get(AhsSeverity.HIGH)).isEqualTo(1);
        assertThat(view.severityVerteilung().get(AhsSeverity.LOW)).isZero();
    }

    @Test
    @DisplayName("aelteste CRITICAL: waehlt den frueheren createdAt und liefert das Alter in Tagen")
    void aelteste() {
        given(repo.findeQueueNachStatus(any(), any(), any(), any()))
                .willReturn(List.of(
                        eintragMitCve("CVE-2026-0001", AhsSeverity.CRITICAL,
                                AssessmentStatus.PROPOSED, JETZT.minusSeconds(2L * 86_400)),
                        eintragMitCve("CVE-2026-0002", AhsSeverity.CRITICAL,
                                AssessmentStatus.PROPOSED, JETZT.minusSeconds(20L * 86_400))));

        DashboardKpiView view = service.berechne();

        assertThat(view.aeltesteKritisch()).isNotNull();
        assertThat(view.aeltesteKritisch().cveKey()).isEqualTo("CVE-2026-0002");
        assertThat(view.aeltesteKritisch().tage()).isEqualTo(20);
    }

    @Test
    @DisplayName("weiterbetriebOk=false, sobald ein offenes CRITICAL aelter als die Schwelle ist")
    void weiterbetriebKritisch() {
        given(repo.findeQueueNachStatus(any(), any(), any(), any()))
                .willReturn(List.of(
                        eintrag(AhsSeverity.CRITICAL, AssessmentStatus.PROPOSED,
                                JETZT.minusSeconds(20L * 86_400))));

        DashboardKpiView view = service.berechne();

        assertThat(view.weiterbetriebOk()).isFalse();
    }

    @Test
    @DisplayName("weiterbetriebOk=true, wenn CRITICAL juenger als die Schwelle ist")
    void weiterbetriebOk() {
        given(repo.findeQueueNachStatus(any(), any(), any(), any()))
                .willReturn(List.of(
                        eintrag(AhsSeverity.CRITICAL, AssessmentStatus.PROPOSED,
                                JETZT.minusSeconds(2L * 86_400))));

        DashboardKpiView view = service.berechne();

        assertThat(view.weiterbetriebOk()).isTrue();
    }

    private Assessment eintrag(AhsSeverity severity, AssessmentStatus status, Instant created) {
        return eintragMitCve("CVE-X", severity, status, created);
    }

    private Assessment eintragMitCve(
            String cveKey, AhsSeverity severity, AssessmentStatus status, Instant created) {
        Cve cve = Cve.builder().id(UUID.randomUUID()).cveId(cveKey).build();
        return Assessment.builder()
                .id(UUID.randomUUID())
                .finding(Finding.builder().id(UUID.randomUUID()).cve(cve).build())
                .productVersion(ProductVersion.builder().id(UUID.randomUUID()).build())
                .environment(Environment.builder().id(UUID.randomUUID()).build())
                .cve(cve)
                .severity(severity)
                .status(status)
                .proposalSource(ProposalSource.RULE)
                .version(1)
                .createdAt(created)
                .build();
    }
}
