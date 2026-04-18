package com.ahs.cvm.application.waiver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.waiver.WaiverService.GrantCommand;
import com.ahs.cvm.application.waiver.WaiverService.VierAugenViolationException;
import com.ahs.cvm.domain.enums.MitigationStatus;
import com.ahs.cvm.domain.enums.MitigationStrategy;
import com.ahs.cvm.domain.enums.WaiverStatus;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.mitigation.MitigationPlan;
import com.ahs.cvm.persistence.mitigation.MitigationPlanRepository;
import com.ahs.cvm.persistence.waiver.Waiver;
import com.ahs.cvm.persistence.waiver.WaiverRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WaiverServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-18T10:00:00Z");
    private static final UUID ASSESSMENT_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private WaiverRepository waiverRepo;
    private AssessmentRepository assessmentRepo;
    private MitigationPlanRepository mitRepo;
    private WaiverService service;

    @BeforeEach
    void setUp() {
        waiverRepo = mock(WaiverRepository.class);
        assessmentRepo = mock(AssessmentRepository.class);
        mitRepo = mock(MitigationPlanRepository.class);
        given(waiverRepo.save(any(Waiver.class))).willAnswer(inv -> {
            Waiver w = inv.getArgument(0);
            if (w.getId() == null) {
                w.setId(UUID.randomUUID());
            }
            return w;
        });
        service = new WaiverService(waiverRepo, assessmentRepo, mitRepo,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Assessment assessmentWith(String decidedBy) {
        return Assessment.builder()
                .id(ASSESSMENT_ID)
                .decidedBy(decidedBy)
                .build();
    }

    private MitigationPlan plan(MitigationStrategy strategy) {
        return MitigationPlan.builder()
                .id(UUID.randomUUID())
                .strategy(strategy)
                .status(MitigationStatus.PLANNED)
                .build();
    }

    @Test
    @DisplayName("Waiver: Anlage fuer ACCEPT_RISK-Assessment funktioniert")
    void grantAcceptRisk() {
        given(assessmentRepo.findById(ASSESSMENT_ID)).willReturn(
                Optional.of(assessmentWith("t.tester@ahs.test")));
        given(mitRepo.findByAssessmentId(ASSESSMENT_ID)).willReturn(
                List.of(plan(MitigationStrategy.ACCEPT_RISK)));

        WaiverView v = service.grant(new GrantCommand(
                ASSESSMENT_ID, "dokumentiertes Restrisiko",
                "a.admin@ahs.test",
                NOW.plusSeconds(60L * 60 * 24 * 90),
                90));

        assertThat(v.status()).isEqualTo(WaiverStatus.ACTIVE);
        assertThat(v.grantedBy()).isEqualTo("a.admin@ahs.test");
    }

    @Test
    @DisplayName("Waiver: Anlage schlaegt fehl bei falscher Strategy")
    void grantFalscheStrategy() {
        given(assessmentRepo.findById(ASSESSMENT_ID)).willReturn(
                Optional.of(assessmentWith("t.tester@ahs.test")));
        given(mitRepo.findByAssessmentId(ASSESSMENT_ID)).willReturn(
                List.of(plan(MitigationStrategy.UPGRADE)));

        assertThatThrownBy(() -> service.grant(new GrantCommand(
                        ASSESSMENT_ID, "r", "a.admin@ahs.test",
                        NOW.plusSeconds(86400), 30)))
                .isInstanceOf(WaiverNotApplicableException.class);
    }

    @Test
    @DisplayName("Waiver: Vier-Augen greift (grantedBy == decidedBy)")
    void grantVierAugen() {
        given(assessmentRepo.findById(ASSESSMENT_ID)).willReturn(
                Optional.of(assessmentWith("a.admin@ahs.test")));
        given(mitRepo.findByAssessmentId(ASSESSMENT_ID)).willReturn(
                List.of(plan(MitigationStrategy.ACCEPT_RISK)));

        assertThatThrownBy(() -> service.grant(new GrantCommand(
                        ASSESSMENT_ID, "r", "a.admin@ahs.test",
                        NOW.plusSeconds(86400), 30)))
                .isInstanceOf(VierAugenViolationException.class);
    }

    @Test
    @DisplayName("Waiver: extend setzt validUntil und Status ACTIVE, Vier-Augen greift")
    void extend() {
        UUID id = UUID.randomUUID();
        Waiver w = Waiver.builder()
                .id(id)
                .grantedBy("a.admin@ahs.test")
                .status(WaiverStatus.EXPIRING_SOON)
                .validUntil(NOW.plusSeconds(100))
                .build();
        given(waiverRepo.findById(id)).willReturn(Optional.of(w));

        // Vier-Augen-Verstoss
        assertThatThrownBy(() -> service.extend(id, NOW.plusSeconds(86400), "a.admin@ahs.test"))
                .isInstanceOf(VierAugenViolationException.class);

        // Happy-Path mit anderem User
        WaiverView v = service.extend(id, NOW.plusSeconds(86400), "b.boss@ahs.test");
        assertThat(v.status()).isEqualTo(WaiverStatus.ACTIVE);
        assertThat(v.extendedAt()).isNotNull();
    }

    @Test
    @DisplayName("Waiver: revoke setzt REVOKED, Kommentar wird angehaengt")
    void revoke() {
        UUID id = UUID.randomUUID();
        Waiver w = Waiver.builder()
                .id(id).grantedBy("x").status(WaiverStatus.ACTIVE)
                .reason("original").validUntil(NOW.plusSeconds(100)).build();
        given(waiverRepo.findById(id)).willReturn(Optional.of(w));

        WaiverView v = service.revoke(id, "a.admin@ahs.test", "doppelt");

        assertThat(v.status()).isEqualTo(WaiverStatus.REVOKED);
        assertThat(w.getReason()).contains("[REVOKED] doppelt");
    }
}
