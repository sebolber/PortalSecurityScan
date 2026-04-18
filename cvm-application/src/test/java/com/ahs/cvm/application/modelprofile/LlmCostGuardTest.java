package com.ahs.cvm.application.modelprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.persistence.ai.AiCallAuditRepository;
import com.ahs.cvm.persistence.modelprofile.LlmModelProfile;
import com.ahs.cvm.persistence.modelprofile.LlmModelProfile.Provider;
import com.ahs.cvm.persistence.modelprofile.LlmModelProfileRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LlmCostGuardTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-04-18T10:00:00Z");

    private LlmModelProfileRepository profileRepo;
    private AiCallAuditRepository auditRepo;
    private LlmCostGuard guard;

    @BeforeEach
    void setUp() {
        profileRepo = mock(LlmModelProfileRepository.class);
        auditRepo = mock(AiCallAuditRepository.class);
        guard = new LlmCostGuard(profileRepo, auditRepo,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private LlmModelProfile profile(BigDecimal budget) {
        return LlmModelProfile.builder()
                .id(PROFILE_ID)
                .profileKey("claude-api-prod")
                .provider(Provider.CLAUDE_CLOUD)
                .modelId("claude-opus-4-7")
                .costBudgetEurMonthly(budget)
                .approvedForGkvData(true)
                .build();
    }

    @Test
    @DisplayName("Cost-Cap: unter Budget -> true")
    void unterBudget() {
        given(profileRepo.findById(PROFILE_ID)).willReturn(Optional.of(
                profile(new BigDecimal("100.00"))));
        given(auditRepo.sumCostEurForModelAndRange(
                eq("claude-opus-4-7"), any(), any()))
                .willReturn(new BigDecimal("12.50"));

        assertThat(guard.isUnderBudget(PROFILE_ID)).isTrue();
    }

    @Test
    @DisplayName("Cost-Cap: Budget gerissen -> false")
    void ueberBudget() {
        given(profileRepo.findById(PROFILE_ID)).willReturn(Optional.of(
                profile(new BigDecimal("10.00"))));
        given(auditRepo.sumCostEurForModelAndRange(
                eq("claude-opus-4-7"), any(), any()))
                .willReturn(new BigDecimal("10.00"));

        assertThat(guard.isUnderBudget(PROFILE_ID)).isFalse();
    }

    @Test
    @DisplayName("Cost-Cap: Budget 0 -> unbegrenzt")
    void budgetNull() {
        given(profileRepo.findById(PROFILE_ID)).willReturn(Optional.of(
                profile(BigDecimal.ZERO)));

        assertThat(guard.isUnderBudget(PROFILE_ID)).isTrue();
    }

    @Test
    @DisplayName("Cost-Cap: null-Profil -> unbegrenzt")
    void profilNull() {
        assertThat(guard.isUnderBudget(null)).isTrue();
    }

    @Test
    @DisplayName("Cost-Cap: unbekanntes Profil -> unbegrenzt (mit Warnung)")
    void unbekanntesProfil() {
        given(profileRepo.findById(PROFILE_ID)).willReturn(Optional.empty());
        assertThat(guard.isUnderBudget(PROFILE_ID)).isTrue();
    }

    @Test
    @DisplayName("Cost-Cap: Monatsfenster ist Beginn bis Beginn-naechster-Monat")
    void monatsFenster() {
        LlmCostGuard.Range r = guard.rangeForCurrentMonth();
        assertThat(r.from()).isEqualTo(Instant.parse("2026-04-01T00:00:00Z"));
        assertThat(r.to()).isEqualTo(Instant.parse("2026-05-01T00:00:00Z"));
    }
}
