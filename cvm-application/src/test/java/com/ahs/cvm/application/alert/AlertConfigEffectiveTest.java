package com.ahs.cvm.application.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlertConfigEffectiveTest {

    @Test
    @DisplayName("Ohne Resolver: Boot-Defaults werden zurueckgegeben")
    void ohne_resolver_defaults() {
        AlertConfig cfg = new AlertConfig("real", 60, 180, "alerts@ahs.local");
        assertThat(cfg.dryRunEffective()).isFalse();
        assertThat(cfg.t1Effective()).isEqualTo(Duration.ofMinutes(60));
        assertThat(cfg.t2Effective()).isEqualTo(Duration.ofMinutes(180));
        assertThat(cfg.fromAddressEffective()).isEqualTo("alerts@ahs.local");
    }

    @Test
    @DisplayName("Resolver-Override setzt mode auf 'live' und verschiebt T1/T2")
    void resolver_override() {
        AlertConfig cfg = new AlertConfig("dry-run", 120, 360, "cvm-alerts@ahs.local");
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveString("cvm.alerts.mode", "dry-run")).willReturn("live");
        given(resolver.resolveInt("cvm.alerts.eskalation.t1-minutes", 120)).willReturn(30);
        given(resolver.resolveInt("cvm.alerts.eskalation.t2-minutes", 360)).willReturn(90);
        given(resolver.resolveString("cvm.alerts.from", "cvm-alerts@ahs.local"))
                .willReturn("ops@ahs.local");
        cfg.setResolver(resolver);

        assertThat(cfg.dryRunEffective()).isFalse();
        assertThat(cfg.t1Effective()).isEqualTo(Duration.ofMinutes(30));
        assertThat(cfg.t2Effective()).isEqualTo(Duration.ofMinutes(90));
        assertThat(cfg.fromAddressEffective()).isEqualTo("ops@ahs.local");
    }

    @Test
    @DisplayName("T2 wird mindestens auf T1+1 gehoben, wenn der Override T2 <= T1 setzt")
    void t2_mindestens_t1_plus_1() {
        AlertConfig cfg = new AlertConfig("dry-run", 120, 360, "cvm-alerts@ahs.local");
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveInt("cvm.alerts.eskalation.t1-minutes", 120)).willReturn(60);
        given(resolver.resolveInt("cvm.alerts.eskalation.t2-minutes", 360)).willReturn(30);
        cfg.setResolver(resolver);

        assertThat(cfg.t1Effective()).isEqualTo(Duration.ofMinutes(60));
        assertThat(cfg.t2Effective()).isEqualTo(Duration.ofMinutes(61));
    }

    @Test
    @DisplayName("mode='dry-run' und mode='log' fuehren beide zu dryRunEffective=true")
    void dry_run_und_log_sind_dry() {
        AlertConfig cfg = new AlertConfig("dry-run", 120, 360, "cvm-alerts@ahs.local");
        SystemParameterResolver resolver = mock(SystemParameterResolver.class);
        given(resolver.resolveString("cvm.alerts.mode", "dry-run")).willReturn("log");
        cfg.setResolver(resolver);
        assertThat(cfg.dryRunEffective()).isTrue();
    }
}
