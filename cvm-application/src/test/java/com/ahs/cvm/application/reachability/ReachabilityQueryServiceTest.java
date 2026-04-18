package com.ahs.cvm.application.reachability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AiSuggestionStatus;
import com.ahs.cvm.persistence.ai.AiCallAudit;
import com.ahs.cvm.persistence.ai.AiSuggestion;
import com.ahs.cvm.persistence.ai.AiSuggestionRepository;
import com.ahs.cvm.persistence.finding.Finding;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class ReachabilityQueryServiceTest {

    private AiSuggestionRepository repo;
    private ReachabilityQueryService service;

    @BeforeEach
    void setUp() {
        repo = mock(AiSuggestionRepository.class);
        service = new ReachabilityQueryService(repo);
    }

    @Test
    @DisplayName("recent: ruft Repository mit Use-Case REACHABILITY auf")
    void delegiertAnRepo() {
        given(repo.findByUseCaseOrderByCreatedAtDesc(
                        eq(ReachabilityQueryService.USE_CASE), any(Pageable.class)))
                .willReturn(List.of(suggestion()));

        List<ReachabilitySummaryView> views = service.recent(25);
        assertThat(views).hasSize(1);
        assertThat(views.get(0).severity()).isEqualTo(AhsSeverity.HIGH);
    }

    @Test
    @DisplayName("recent: limit wird auf MAX_LIMIT gekappt")
    void limitWirdGekappt() {
        given(repo.findByUseCaseOrderByCreatedAtDesc(any(), any(Pageable.class)))
                .willReturn(List.of());
        service.recent(10_000);
        assertThat(ReachabilityQueryService.MAX_LIMIT).isEqualTo(500);
    }

    private static AiSuggestion suggestion() {
        AiCallAudit audit = AiCallAudit.builder().id(UUID.randomUUID()).build();
        Finding finding = Finding.builder().id(UUID.randomUUID()).build();
        return AiSuggestion.builder()
                .id(UUID.randomUUID())
                .aiCallAudit(audit)
                .useCase("REACHABILITY")
                .finding(finding)
                .severity(AhsSeverity.HIGH)
                .status(AiSuggestionStatus.PROPOSED)
                .rationale("Call-Site nachgewiesen")
                .confidence(new BigDecimal("0.85"))
                .createdAt(Instant.now())
                .build();
    }
}
