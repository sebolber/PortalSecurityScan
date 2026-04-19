package com.ahs.cvm.application.reachability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AiSuggestionStatus;
import com.ahs.cvm.persistence.ai.AiCallAudit;
import com.ahs.cvm.persistence.ai.AiSuggestion;
import com.ahs.cvm.persistence.ai.AiSuggestionRepository;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.scan.Component;
import com.ahs.cvm.persistence.scan.ComponentOccurrence;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class ReachabilityQueryServiceTest {

    private AiSuggestionRepository repo;
    private FindingRepository findingRepo;
    private ReachabilityQueryService service;

    @BeforeEach
    void setUp() {
        repo = mock(AiSuggestionRepository.class);
        findingRepo = mock(FindingRepository.class);
        service = new ReachabilityQueryService(repo, findingRepo);
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

    @Test
    @DisplayName("suggestionForFinding: leitet Symbol aus Component-PURL ab")
    void suggestionAusPurl() {
        UUID id = UUID.randomUUID();
        Component comp = Component.builder()
                .purl("pkg:maven/org.apache.commons/commons-text@1.9")
                .build();
        ComponentOccurrence occ = ComponentOccurrence.builder()
                .component(comp).build();
        Finding finding = Finding.builder().id(id).componentOccurrence(occ).build();
        given(findingRepo.findById(id)).willReturn(Optional.of(finding));

        ReachabilitySuggestionView view = service.suggestionForFinding(id);

        assertThat(view.findingId()).isEqualTo(id);
        assertThat(view.symbol()).isEqualTo("org.apache.commons.text");
        assertThat(view.language()).isEqualTo("java");
        assertThat(view.sourcePurl()).startsWith("pkg:maven/");
    }

    @Test
    @DisplayName("suggestionForFinding: unbekannte PURL liefert null-Symbol mit erklaerender Ratione")
    void suggestionUnbekanntePurl() {
        UUID id = UUID.randomUUID();
        Component comp = Component.builder().purl("pkg:generic/foo@1").build();
        ComponentOccurrence occ = ComponentOccurrence.builder()
                .component(comp).build();
        Finding finding = Finding.builder().id(id).componentOccurrence(occ).build();
        given(findingRepo.findById(id)).willReturn(Optional.of(finding));

        ReachabilitySuggestionView view = service.suggestionForFinding(id);

        assertThat(view.symbol()).isNull();
        assertThat(view.language()).isNull();
        assertThat(view.rationale()).containsIgnoringCase("manuell");
    }

    @Test
    @DisplayName("suggestionForFinding: Finding existiert nicht -> FindingNotFoundException")
    void suggestionFindingFehlt() {
        UUID id = UUID.randomUUID();
        given(findingRepo.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.suggestionForFinding(id))
                .isInstanceOf(ReachabilityQueryService.FindingNotFoundException.class);
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
