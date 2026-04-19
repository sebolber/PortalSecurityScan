package com.ahs.cvm.ai.ruleextraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuleExtractionConfigEffectiveTest {

    @Test
    @DisplayName("Ohne Resolver: Boot-Defaults werden zurueckgegeben")
    void ohne_resolver_defaults() {
        RuleExtractionConfig cfg = new RuleExtractionConfig(true, 180, 10, 4);
        assertThat(cfg.enabledEffective()).isTrue();
        assertThat(cfg.windowDaysEffective()).isEqualTo(180);
        assertThat(cfg.clusterCapEffective()).isEqualTo(10);
        assertThat(cfg.overrideReviewThresholdEffective()).isEqualTo(4);
    }

    @Test
    @DisplayName("Resolver-Override aendert alle vier Felder")
    void resolver_override() {
        RuleExtractionConfig cfg = new RuleExtractionConfig(false, 180, 10, 4);
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveBoolean("cvm.ai.rule-extraction.enabled", false)).willReturn(true);
        given(resolver.resolveInt("cvm.ai.rule-extraction.window-days", 180)).willReturn(360);
        given(resolver.resolveInt("cvm.ai.rule-extraction.cluster-cap", 10)).willReturn(20);
        given(resolver.resolveInt("cvm.ai.rule-extraction.override-review-threshold", 4)).willReturn(2);
        cfg.setResolver(resolver);

        assertThat(cfg.enabledEffective()).isTrue();
        assertThat(cfg.windowDaysEffective()).isEqualTo(360);
        assertThat(cfg.clusterCapEffective()).isEqualTo(20);
        assertThat(cfg.overrideReviewThresholdEffective()).isEqualTo(2);
    }

    @Test
    @DisplayName("windowDays < 7 wird auf 7 geclamped; clusterCap < 1 auf 1; overrideReviewThreshold < 1 auf 1")
    void clamps_greifen() {
        RuleExtractionConfig cfg = new RuleExtractionConfig(false, 180, 10, 4);
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveInt("cvm.ai.rule-extraction.window-days", 180)).willReturn(3);
        given(resolver.resolveInt("cvm.ai.rule-extraction.cluster-cap", 10)).willReturn(0);
        given(resolver.resolveInt("cvm.ai.rule-extraction.override-review-threshold", 4)).willReturn(0);
        cfg.setResolver(resolver);

        assertThat(cfg.windowDaysEffective()).isEqualTo(7);
        assertThat(cfg.clusterCapEffective()).isEqualTo(1);
        assertThat(cfg.overrideReviewThresholdEffective()).isEqualTo(1);
    }
}
