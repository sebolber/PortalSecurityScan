package com.ahs.cvm.application.fixverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.domain.enums.FixVerificationGrade;
import com.ahs.cvm.domain.enums.MitigationStatus;
import com.ahs.cvm.domain.enums.MitigationStrategy;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.mitigation.MitigationPlan;
import com.ahs.cvm.persistence.mitigation.MitigationPlanRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class FixVerificationQueryServiceTest {

    private MitigationPlanRepository repo;
    private FixVerificationQueryService service;

    @BeforeEach
    void setUp() {
        repo = mock(MitigationPlanRepository.class);
        service = new FixVerificationQueryService(repo);
    }

    @Test
    @DisplayName("recent: begrenzt Limit auf MAX_LIMIT")
    void limitWirdGekappt() {
        given(repo.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .willReturn(List.of());
        service.recent(10_000);
        assertThat(FixVerificationQueryService.MAX_LIMIT).isEqualTo(500);
    }

    @Test
    @DisplayName("recent: liefert Views in Repository-Reihenfolge")
    void recent() {
        given(repo.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .willReturn(List.of(plan(FixVerificationGrade.A), plan(null)));
        List<FixVerificationSummaryView> views = service.recent(50);
        assertThat(views).hasSize(2);
        assertThat(views.get(0).verificationGrade()).isEqualTo(FixVerificationGrade.A);
        assertThat(views.get(1).verificationGrade()).isNull();
    }

    @Test
    @DisplayName("byGrade: delegiert an grade-Repository-Methode")
    void byGrade() {
        given(repo.findByVerificationGradeOrderByCreatedAtDesc(
                        eq(FixVerificationGrade.B), any(Pageable.class)))
                .willReturn(List.of(plan(FixVerificationGrade.B)));
        List<FixVerificationSummaryView> views =
                service.byGrade(FixVerificationGrade.B, 20);
        assertThat(views).hasSize(1);
        assertThat(views.get(0).verificationGrade()).isEqualTo(FixVerificationGrade.B);
    }

    private static MitigationPlan plan(FixVerificationGrade grade) {
        Assessment assessment = Assessment.builder().id(UUID.randomUUID()).build();
        return MitigationPlan.builder()
                .id(UUID.randomUUID())
                .assessment(assessment)
                .strategy(MitigationStrategy.UPGRADE)
                .status(MitigationStatus.PLANNED)
                .verificationGrade(grade)
                .createdAt(Instant.now())
                .build();
    }
}
