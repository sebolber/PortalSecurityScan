package com.ahs.cvm.ai.reachability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReachabilityConfigEffectiveTest {

    @Test
    @DisplayName("Ohne Resolver: Legacy-Werte werden zurueckgegeben")
    void ohne_resolver_legacy_werte() {
        ReachabilityConfig cfg = new ReachabilityConfig(true, 600, "claude");
        assertThat(cfg.enabled()).isTrue();
        assertThat(cfg.timeout()).isEqualTo(Duration.ofSeconds(600));
        assertThat(cfg.binary()).isEqualTo("claude");

        assertThat(cfg.enabledEffective()).isTrue();
        assertThat(cfg.timeoutEffective()).isEqualTo(Duration.ofSeconds(600));
        assertThat(cfg.binaryEffective()).isEqualTo("claude");
    }

    @Test
    @DisplayName("Resolver-Override dreht enabled ohne Neustart um")
    void resolver_override_enabled() {
        ReachabilityConfig cfg = new ReachabilityConfig(false, 300, "claude");
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveBoolean("cvm.ai.reachability.enabled", false)).willReturn(true);
        given(resolver.resolveInt("cvm.ai.reachability.timeout-seconds", 300)).willReturn(900);
        given(resolver.resolveString("cvm.ai.reachability.binary", "claude")).willReturn("/usr/local/bin/claude");
        cfg.setResolver(resolver);

        assertThat(cfg.enabled()).isFalse();
        assertThat(cfg.enabledEffective()).isTrue();
        assertThat(cfg.timeoutEffective()).isEqualTo(Duration.ofSeconds(900));
        assertThat(cfg.binaryEffective()).isEqualTo("/usr/local/bin/claude");
    }

    @Test
    @DisplayName("Leerer Binary-Override faellt auf 'claude' zurueck, Timeout-Minimum bleibt 5 s")
    void defaults_fuer_leere_werte() {
        ReachabilityConfig cfg = new ReachabilityConfig(false, 300, "claude");
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveBoolean("cvm.ai.reachability.enabled", false)).willReturn(false);
        given(resolver.resolveInt("cvm.ai.reachability.timeout-seconds", 300)).willReturn(1);
        given(resolver.resolveString("cvm.ai.reachability.binary", "claude")).willReturn("   ");
        cfg.setResolver(resolver);

        assertThat(cfg.timeoutEffective()).isEqualTo(Duration.ofSeconds(5));
        assertThat(cfg.binaryEffective()).isEqualTo("claude");
    }
}
