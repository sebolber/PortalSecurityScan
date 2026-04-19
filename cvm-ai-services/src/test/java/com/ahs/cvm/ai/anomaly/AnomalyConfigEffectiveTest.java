package com.ahs.cvm.ai.anomaly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnomalyConfigEffectiveTest {

    @Test
    @DisplayName("Ohne Resolver: Boot-Defaults werden zurueckgegeben")
    void ohne_resolver_defaults() {
        AnomalyConfig cfg = new AnomalyConfig(true, 0.7, 5, 0.9, false);
        assertThat(cfg.enabledEffective()).isTrue();
        assertThat(cfg.kevEpssThresholdEffective()).isEqualTo(0.7);
        assertThat(cfg.manyAcceptRiskThresholdEffective()).isEqualTo(5);
        assertThat(cfg.similarRejectionThresholdEffective()).isEqualTo(0.9);
        assertThat(cfg.useLlmSecondStageEffective()).isFalse();
    }

    @Test
    @DisplayName("Resolver-Override aendert alle fuenf Felder zur Laufzeit")
    void resolver_override() {
        AnomalyConfig cfg = new AnomalyConfig(false, 0.7, 5, 0.9, false);
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveBoolean("cvm.ai.anomaly.enabled", false)).willReturn(true);
        given(resolver.resolveDouble("cvm.ai.anomaly.kev-epss-threshold", 0.7)).willReturn(0.5);
        given(resolver.resolveInt("cvm.ai.anomaly.many-accept-risk-threshold", 5)).willReturn(3);
        given(resolver.resolveDouble("cvm.ai.anomaly.similar-rejection-threshold", 0.9)).willReturn(0.8);
        given(resolver.resolveBoolean("cvm.ai.anomaly.use-llm-second-stage", false)).willReturn(true);
        cfg.setResolver(resolver);

        assertThat(cfg.enabledEffective()).isTrue();
        assertThat(cfg.kevEpssThresholdEffective()).isEqualTo(0.5);
        assertThat(cfg.manyAcceptRiskThresholdEffective()).isEqualTo(3);
        assertThat(cfg.similarRejectionThresholdEffective()).isEqualTo(0.8);
        assertThat(cfg.useLlmSecondStageEffective()).isTrue();
    }

    @Test
    @DisplayName("many-accept-risk-threshold < 1 wird auf 1 geclamped")
    void clamp_many_accept() {
        AnomalyConfig cfg = new AnomalyConfig(false, 0.7, 5, 0.9, false);
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveInt("cvm.ai.anomaly.many-accept-risk-threshold", 5)).willReturn(0);
        cfg.setResolver(resolver);

        assertThat(cfg.manyAcceptRiskThresholdEffective()).isEqualTo(1);
    }
}
