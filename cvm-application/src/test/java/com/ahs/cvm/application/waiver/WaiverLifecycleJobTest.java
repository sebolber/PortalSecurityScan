package com.ahs.cvm.application.waiver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.application.alert.AlertContext;
import com.ahs.cvm.application.alert.AlertEvaluator;
import com.ahs.cvm.application.waiver.WaiverLifecycleJob.JobReport;
import com.ahs.cvm.domain.enums.WaiverStatus;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.waiver.Waiver;
import com.ahs.cvm.persistence.waiver.WaiverRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WaiverLifecycleJobTest {

    private static final Instant NOW = Instant.parse("2026-04-18T10:00:00Z");
    private WaiverRepository waiverRepo;
    private AssessmentRepository assessmentRepo;
    private AlertEvaluator alerts;
    private WaiverLifecycleJob job;

    @BeforeEach
    void setUp() {
        waiverRepo = mock(WaiverRepository.class);
        assessmentRepo = mock(AssessmentRepository.class);
        alerts = mock(AlertEvaluator.class);
        job = new WaiverLifecycleJob(
                waiverRepo, assessmentRepo, alerts,
                Clock.fixed(NOW, ZoneOffset.UTC), true);
    }

    private Waiver waiver(WaiverStatus status, Instant validUntil) {
        Assessment a = Assessment.builder().id(UUID.randomUUID()).build();
        return Waiver.builder()
                .id(UUID.randomUUID())
                .assessment(a)
                .status(status)
                .validUntil(validUntil)
                .build();
    }

    @Test
    @DisplayName("Lifecycle: ACTIVE bleibt ACTIVE, wenn validUntil > 30d")
    void bleibtActive() {
        Waiver w = waiver(WaiverStatus.ACTIVE, NOW.plus(Duration.ofDays(60)));
        given(waiverRepo.findByStatus(WaiverStatus.ACTIVE)).willReturn(List.of(w));
        given(waiverRepo.findByStatusAndValidUntilBefore(
                eq(WaiverStatus.EXPIRING_SOON), any())).willReturn(List.of());

        JobReport r = job.runOnce();

        assertThat(r.toExpiringSoon()).isZero();
        assertThat(r.toExpired()).isZero();
    }

    @Test
    @DisplayName("Lifecycle: ACTIVE -> EXPIRING_SOON bei validUntil <= 30d, Alert feuert")
    void activeZuSoon() {
        Waiver w = waiver(WaiverStatus.ACTIVE, NOW.plus(Duration.ofDays(15)));
        given(waiverRepo.findByStatus(WaiverStatus.ACTIVE)).willReturn(List.of(w));
        given(waiverRepo.findByStatusAndValidUntilBefore(
                eq(WaiverStatus.EXPIRING_SOON), any())).willReturn(List.of());

        JobReport r = job.runOnce();

        assertThat(r.toExpiringSoon()).isEqualTo(1);
        assertThat(w.getStatus()).isEqualTo(WaiverStatus.EXPIRING_SOON);
        verify(alerts).evaluate(any(AlertContext.class));
    }

    @Test
    @DisplayName("Lifecycle: EXPIRING_SOON -> EXPIRED setzt Assessment auf NEEDS_REVIEW")
    void soonZuExpired() {
        Waiver w = waiver(WaiverStatus.EXPIRING_SOON, NOW.minus(Duration.ofMinutes(5)));
        given(waiverRepo.findByStatus(WaiverStatus.ACTIVE)).willReturn(List.of());
        given(waiverRepo.findByStatusAndValidUntilBefore(
                eq(WaiverStatus.EXPIRING_SOON), any())).willReturn(List.of(w));

        JobReport r = job.runOnce();

        assertThat(r.toExpired()).isEqualTo(1);
        assertThat(w.getStatus()).isEqualTo(WaiverStatus.EXPIRED);
        verify(assessmentRepo).markiereAlsReview(
                eq(List.of(w.getAssessment().getId())), any());
    }

    @Test
    @DisplayName("Lifecycle: ACTIVE mit bereits abgelaufenem validUntil -> direkt EXPIRED")
    void activeDirekteExpiry() {
        Waiver w = waiver(WaiverStatus.ACTIVE, NOW.minus(Duration.ofDays(1)));
        given(waiverRepo.findByStatus(WaiverStatus.ACTIVE)).willReturn(List.of(w));
        given(waiverRepo.findByStatusAndValidUntilBefore(
                eq(WaiverStatus.EXPIRING_SOON), any())).willReturn(List.of());

        JobReport r = job.runOnce();

        assertThat(r.toExpired()).isEqualTo(1);
        assertThat(w.getStatus()).isEqualTo(WaiverStatus.EXPIRED);
    }

    @Test
    @DisplayName("Lifecycle: scheduler-disabled -> keine DB-Touches")
    void schedulerAus() {
        job = new WaiverLifecycleJob(
                waiverRepo, assessmentRepo, alerts,
                Clock.fixed(NOW, ZoneOffset.UTC), false);
        job.scheduledRun();
        org.mockito.Mockito.verifyNoInteractions(waiverRepo);
    }
}
