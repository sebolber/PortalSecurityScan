package com.ahs.cvm.application.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssessmentConfigEffectiveTest {

    @Test
    @DisplayName("Ohne Resolver: defaultValidMonthsEffective entspricht dem Boot-Default")
    void ohne_resolver_default() {
        AssessmentConfig cfg = new AssessmentConfig(24);
        assertThat(cfg.defaultValidMonthsEffective()).isEqualTo(24);
    }

    @Test
    @DisplayName("Resolver-Override setzt defaultValidMonths ohne Neustart auf 6")
    void resolver_override() {
        AssessmentConfig cfg = new AssessmentConfig(12);
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveInt("cvm.assessment.default-valid-months", 12)).willReturn(6);
        cfg.setResolver(resolver);

        assertThat(cfg.defaultValidMonthsEffective()).isEqualTo(6);
    }

    @Test
    @DisplayName("Nicht-positive Werte fallen auf den Boot-Default zurueck")
    void nicht_positiv_fallback() {
        AssessmentConfig cfg = new AssessmentConfig(12);
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveInt("cvm.assessment.default-valid-months", 12)).willReturn(0);
        cfg.setResolver(resolver);

        assertThat(cfg.defaultValidMonthsEffective()).isEqualTo(12);
    }
}
