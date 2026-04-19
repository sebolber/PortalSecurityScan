package com.ahs.cvm.ai.fixverify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FixVerificationConfigEffectiveTest {

    @Test
    @DisplayName("Ohne Resolver: Boot-Defaults werden zurueckgegeben")
    void ohne_resolver_defaults() {
        FixVerificationConfig cfg = new FixVerificationConfig(true, 80, 720);
        assertThat(cfg.enabledEffective()).isTrue();
        assertThat(cfg.fullTextCommitCapEffective()).isEqualTo(80);
        assertThat(cfg.cacheTtlMinutesEffective()).isEqualTo(720);
    }

    @Test
    @DisplayName("Resolver-Override dreht enabled und erhoeht Cap")
    void resolver_override() {
        FixVerificationConfig cfg = new FixVerificationConfig(false, 50, 1440);
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveBoolean("cvm.ai.fix-verification.enabled", false)).willReturn(true);
        given(resolver.resolveInt("cvm.ai.fix-verification.full-text-commit-cap", 50)).willReturn(120);
        given(resolver.resolveInt("cvm.ai.fix-verification.cache-ttl-minutes", 1440)).willReturn(60);
        cfg.setResolver(resolver);

        assertThat(cfg.enabledEffective()).isTrue();
        assertThat(cfg.fullTextCommitCapEffective()).isEqualTo(120);
        assertThat(cfg.cacheTtlMinutesEffective()).isEqualTo(60);
    }

    @Test
    @DisplayName("Minimum-Clamps greifen auch beim Effective-Pfad")
    void minimum_clamps() {
        FixVerificationConfig cfg = new FixVerificationConfig(false, 50, 1440);
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveInt("cvm.ai.fix-verification.full-text-commit-cap", 50)).willReturn(1);
        given(resolver.resolveInt("cvm.ai.fix-verification.cache-ttl-minutes", 1440)).willReturn(2);
        cfg.setResolver(resolver);

        assertThat(cfg.fullTextCommitCapEffective()).isEqualTo(5);
        assertThat(cfg.cacheTtlMinutesEffective()).isEqualTo(5);
    }
}
