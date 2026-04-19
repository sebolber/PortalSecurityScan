package com.ahs.cvm.ai.autoassessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AutoAssessmentConfigEffectiveTest {

    @Test
    @DisplayName("Ohne Resolver: Legacy-Werte werden zurueckgegeben")
    void ohne_resolver_legacy_werte() {
        AutoAssessmentConfig cfg = new AutoAssessmentConfig(true, 7, 0.55);
        assertThat(cfg.enabled()).isTrue();
        assertThat(cfg.topK()).isEqualTo(7);
        assertThat(cfg.minRagScore()).isEqualTo(0.55);

        assertThat(cfg.enabledEffective()).isTrue();
        assertThat(cfg.topKEffective()).isEqualTo(7);
        assertThat(cfg.minRagScoreEffective()).isEqualTo(0.55);
    }

    @Test
    @DisplayName("Resolver-Override aendert topK und minRagScore zur Laufzeit")
    void resolver_override() {
        AutoAssessmentConfig cfg = new AutoAssessmentConfig(false, 5, 0.6);
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveBoolean("cvm.ai.auto-assessment.enabled", false)).willReturn(true);
        given(resolver.resolveInt("cvm.ai.auto-assessment.top-k", 5)).willReturn(12);
        given(resolver.resolveDouble("cvm.ai.auto-assessment.min-rag-score", 0.6)).willReturn(0.8);
        cfg.setResolver(resolver);

        assertThat(cfg.enabledEffective()).isTrue();
        assertThat(cfg.topKEffective()).isEqualTo(12);
        assertThat(cfg.minRagScoreEffective()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("topK unter 1 wird auf 1 geclamped")
    void top_k_minimum() {
        AutoAssessmentConfig cfg = new AutoAssessmentConfig(true, 5, 0.6);
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveInt("cvm.ai.auto-assessment.top-k", 5)).willReturn(0);
        cfg.setResolver(resolver);

        assertThat(cfg.topKEffective()).isEqualTo(1);
    }
}
