package com.ahs.cvm.application.kpi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.kpi.KpiService.KpiResult;
import com.ahs.cvm.application.kpi.KpiService.SlaBucket;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.product.ProductVersion;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KpiServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-18T10:00:00Z");
    private static final UUID PV = UUID.randomUUID();
    private static final UUID ENV = UUID.randomUUID();

    private AssessmentRepository repo;
    private KpiService service;

    @BeforeEach
    void setUp() {
        repo = mock(AssessmentRepository.class);
        service = new KpiService(repo, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Assessment a(AhsSeverity severity, AssessmentStatus status,
            Instant createdAt, Instant decidedAt, ProposalSource source) {
        Environment env = Environment.builder().id(ENV).key("K").name("N").build();
        ProductVersion pv = ProductVersion.builder().id(PV).build();
        return Assessment.builder()
                .id(UUID.randomUUID())
                .productVersion(pv).environment(env)
                .version(1)
                .severity(severity)
                .status(status)
                .proposalSource(source == null ? ProposalSource.RULE : source)
                .createdAt(createdAt)
                .decidedAt(decidedAt)
                .build();
    }

    @Test
    @DisplayName("KPIs: offene Assessments je Severity zaehlen korrekt")
    void offen() {
        given(repo.findAll()).willReturn(List.of(
                a(AhsSeverity.CRITICAL, AssessmentStatus.PROPOSED, NOW.minus(Duration.ofDays(2)), null, null),
                a(AhsSeverity.CRITICAL, AssessmentStatus.APPROVED, NOW.minus(Duration.ofDays(2)), NOW.minus(Duration.ofDays(1)), null),
                a(AhsSeverity.HIGH, AssessmentStatus.NEEDS_REVIEW, NOW.minus(Duration.ofDays(5)), null, null)
        ));
        KpiResult r = service.compute(PV, ENV, Duration.ofDays(30));
        assertThat(r.openBySeverity().get(AhsSeverity.CRITICAL)).isEqualTo(1L);
        assertThat(r.openBySeverity().get(AhsSeverity.HIGH)).isEqualTo(1L);
        assertThat(r.openBySeverity().get(AhsSeverity.MEDIUM)).isEqualTo(0L);
    }

    @Test
    @DisplayName("KPIs: MTTR als Durchschnitt der abgeschlossenen Tage")
    void mttr() {
        given(repo.findAll()).willReturn(List.of(
                a(AhsSeverity.HIGH, AssessmentStatus.APPROVED,
                        NOW.minus(Duration.ofDays(10)),
                        NOW.minus(Duration.ofDays(2)), null),
                a(AhsSeverity.HIGH, AssessmentStatus.REJECTED,
                        NOW.minus(Duration.ofDays(4)),
                        NOW.minus(Duration.ofDays(2)), null)
        ));
        KpiResult r = service.compute(PV, ENV, Duration.ofDays(30));
        assertThat(r.mttrDaysBySeverity().get(AhsSeverity.HIGH)).isEqualTo(5L);
    }

    @Test
    @DisplayName("KPIs: SLA-Quote CRITICAL mit <=7 Tagen, eine Verletzung")
    void slaCritical() {
        given(repo.findAll()).willReturn(List.of(
                a(AhsSeverity.CRITICAL, AssessmentStatus.APPROVED,
                        NOW.minus(Duration.ofDays(5)),
                        NOW.minus(Duration.ofDays(1)), null),
                a(AhsSeverity.CRITICAL, AssessmentStatus.APPROVED,
                        NOW.minus(Duration.ofDays(20)),
                        NOW.minus(Duration.ofDays(1)), null)
        ));
        KpiResult r = service.compute(PV, ENV, Duration.ofDays(60));
        SlaBucket b = r.slaBySeverity().get(AhsSeverity.CRITICAL);
        assertThat(b.gesamt()).isEqualTo(2);
        assertThat(b.inSla()).isEqualTo(1);
        assertThat(b.quote()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("KPIs: Automatisierungsquote = approved KI / gesamt KI")
    void automation() {
        given(repo.findAll()).willReturn(List.of(
                a(AhsSeverity.MEDIUM, AssessmentStatus.APPROVED,
                        NOW.minus(Duration.ofDays(5)),
                        NOW.minus(Duration.ofDays(1)), ProposalSource.AI_SUGGESTION),
                a(AhsSeverity.MEDIUM, AssessmentStatus.REJECTED,
                        NOW.minus(Duration.ofDays(5)),
                        NOW.minus(Duration.ofDays(1)), ProposalSource.AI_SUGGESTION),
                a(AhsSeverity.MEDIUM, AssessmentStatus.APPROVED,
                        NOW.minus(Duration.ofDays(5)),
                        NOW.minus(Duration.ofDays(1)), ProposalSource.RULE)
        ));
        KpiResult r = service.compute(PV, ENV, Duration.ofDays(30));
        assertThat(r.automationRate()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("KPIs: Burn-Down enthaelt einen Punkt pro Tag im Fenster")
    void burnDown() {
        given(repo.findAll()).willReturn(List.of(
                a(AhsSeverity.HIGH, AssessmentStatus.PROPOSED,
                        NOW.minus(Duration.ofDays(3)), null, null)
        ));
        KpiResult r = service.compute(PV, ENV, Duration.ofDays(5));
        assertThat(r.burnDown()).hasSize(6);
        assertThat(r.burnDown().get(r.burnDown().size() - 1).open()).isEqualTo(1);
    }

    @Test
    @DisplayName("KPIs: Filter auf productVersionId wirkt")
    void filterPv() {
        UUID otherPv = UUID.randomUUID();
        Assessment matching = a(AhsSeverity.HIGH, AssessmentStatus.PROPOSED,
                NOW.minus(Duration.ofDays(1)), null, null);
        Assessment other = a(AhsSeverity.HIGH, AssessmentStatus.PROPOSED,
                NOW.minus(Duration.ofDays(1)), null, null);
        other.setProductVersion(ProductVersion.builder().id(otherPv).build());
        given(repo.findAll()).willReturn(List.of(matching, other));

        KpiResult r = service.compute(PV, ENV, Duration.ofDays(30));
        assertThat(r.openBySeverity().get(AhsSeverity.HIGH)).isEqualTo(1L);
    }
}
