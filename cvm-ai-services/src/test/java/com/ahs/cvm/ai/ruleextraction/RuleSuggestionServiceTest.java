package com.ahs.cvm.ai.ruleextraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.RuleOrigin;
import com.ahs.cvm.domain.enums.RuleStatus;
import com.ahs.cvm.domain.enums.RuleSuggestionStatus;
import com.ahs.cvm.persistence.ai.AiSuggestion;
import com.ahs.cvm.persistence.rule.Rule;
import com.ahs.cvm.persistence.rule.RuleRepository;
import com.ahs.cvm.persistence.rulesuggestion.RuleSuggestion;
import com.ahs.cvm.persistence.rulesuggestion.RuleSuggestionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RuleSuggestionServiceTest {

    private RuleSuggestionRepository suggestionRepo;
    private RuleRepository ruleRepo;
    private RuleSuggestionService service;

    @BeforeEach
    void setUp() {
        suggestionRepo = mock(RuleSuggestionRepository.class);
        ruleRepo = mock(RuleRepository.class);
        given(ruleRepo.save(any(Rule.class))).willAnswer(inv -> {
            Rule r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            return r;
        });
        given(suggestionRepo.save(any(RuleSuggestion.class))).willAnswer(inv -> inv.getArgument(0));
        service = new RuleSuggestionService(suggestionRepo, ruleRepo);
    }

    private RuleSuggestion proposal(String suggestedBy) {
        AiSuggestion ai = AiSuggestion.builder().id(UUID.randomUUID()).build();
        return RuleSuggestion.builder()
                .id(UUID.randomUUID())
                .aiSuggestion(ai)
                .name("ai-test-rule")
                .conditionJson("{\"all\":[]}")
                .proposedSeverity(AhsSeverity.LOW)
                .rationaleTemplate("tpl")
                .clusterRationale("cluster")
                .historicalMatchCount(5)
                .wouldHaveCovered(5)
                .coverageRate(BigDecimal.ONE)
                .conflictCount(0)
                .status(RuleSuggestionStatus.PROPOSED)
                .suggestedBy(suggestedBy)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Approve: erzeugt aktive Rule mit origin=AI_EXTRACTED und Verweis auf ai_suggestion")
    void approveErzeugtRule() {
        RuleSuggestion s = proposal("system:rule-extraction");
        given(suggestionRepo.findById(s.getId())).willReturn(Optional.of(s));

        Rule r = service.approve(s.getId(), "a.admin@ahs.test");

        assertThat(r.getStatus()).isEqualTo(RuleStatus.ACTIVE);
        assertThat(r.getOrigin()).isEqualTo(RuleOrigin.AI_EXTRACTED);
        assertThat(r.getExtractedFromAiSuggestionId()).isEqualTo(s.getAiSuggestion().getId());
        ArgumentCaptor<RuleSuggestion> cap = ArgumentCaptor.forClass(RuleSuggestion.class);
        verify(suggestionRepo).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(RuleSuggestionStatus.APPROVED);
        assertThat(cap.getValue().getApprovedBy()).isEqualTo("a.admin@ahs.test");
    }

    @Test
    @DisplayName("Approve: Vier-Augen greift, wenn approver==suggester")
    void vierAugen() {
        RuleSuggestion s = proposal("a.admin@ahs.test");
        given(suggestionRepo.findById(s.getId())).willReturn(Optional.of(s));

        assertThatThrownBy(() -> service.approve(s.getId(), "a.admin@ahs.test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Vier-Augen");
    }

    @Test
    @DisplayName("Approve: nicht-PROPOSED -> IllegalStateException")
    void nichtProposed() {
        RuleSuggestion s = proposal("sys");
        s.setStatus(RuleSuggestionStatus.APPROVED);
        given(suggestionRepo.findById(s.getId())).willReturn(Optional.of(s));

        assertThatThrownBy(() -> service.approve(s.getId(), "a.admin@ahs.test"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Reject: erfordert Kommentar und setzt Status REJECTED")
    void reject() {
        RuleSuggestion s = proposal("sys");
        given(suggestionRepo.findById(s.getId())).willReturn(Optional.of(s));

        RuleSuggestion out = service.reject(s.getId(), "a.admin@ahs.test", "duplicate");

        assertThat(out.getStatus()).isEqualTo(RuleSuggestionStatus.REJECTED);
        assertThat(out.getRejectedBy()).isEqualTo("a.admin@ahs.test");
        assertThat(out.getRejectComment()).isEqualTo("duplicate");
    }

    @Test
    @DisplayName("Reject: leerer Kommentar -> IllegalArgumentException")
    void rejectOhneKommentar() {
        RuleSuggestion s = proposal("sys");
        given(suggestionRepo.findById(s.getId())).willReturn(Optional.of(s));

        assertThatThrownBy(() -> service.reject(s.getId(), "a.admin@ahs.test", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
