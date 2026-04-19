package com.ahs.cvm.integration.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeedEffectivePropertiesTest {

    private FeedProperties props;
    private SystemParameterResolver resolver;
    private FeedEffectiveProperties effective;

    @BeforeEach
    void setUp() {
        props = new FeedProperties();
        resolver = mock(SystemParameterResolver.class);
        // Default: alle Resolver-Aufrufe geben den Fallback zurueck
        given(resolver.resolveBoolean(anyString(), anyBoolean())).willAnswer(inv -> inv.getArgument(1));
        given(resolver.resolveInt(anyString(), anyInt())).willAnswer(inv -> inv.getArgument(1));
        effective = new FeedEffectiveProperties(props, resolver);
    }

    @Test
    @DisplayName("Ohne DB-Override liefert nvd() die FeedProperties-Defaults (enabled=true, 50/30 s)")
    void default_nvd() {
        FeedEffectiveProperties.EffectiveFeed nvd = effective.nvd();
        assertThat(nvd.enabled()).isTrue();
        assertThat(nvd.requestsPerWindow()).isEqualTo(50);
        assertThat(nvd.windowSeconds()).isEqualTo(30);
        assertThat(nvd.baseUrl()).isEqualTo("https://services.nvd.nist.gov");
    }

    @Test
    @DisplayName("Ohne DB-Override liefert ghsa() den Default enabled=false")
    void default_ghsa_deaktiviert() {
        assertThat(effective.ghsa().enabled()).isFalse();
    }

    @Test
    @DisplayName("DB-Override schaltet KEV aus und halbiert die Requests-Rate")
    void kev_override() {
        given(resolver.resolveBoolean("cvm.feed.kev.enabled", true)).willReturn(false);
        given(resolver.resolveInt("cvm.feed.kev.requests-per-window", 60)).willReturn(30);
        given(resolver.resolveInt("cvm.feed.kev.window-seconds", 60)).willReturn(60);

        FeedEffectiveProperties.EffectiveFeed kev = effective.kev();
        assertThat(kev.enabled()).isFalse();
        assertThat(kev.requestsPerWindow()).isEqualTo(30);
        assertThat(kev.windowSeconds()).isEqualTo(60);
    }

    @Test
    @DisplayName("EPSS-Override aktiviert nichts, aber veraendert Fensterbreite")
    void epss_override() {
        given(resolver.resolveInt("cvm.feed.epss.window-seconds", 60)).willReturn(120);

        FeedEffectiveProperties.EffectiveFeed epss = effective.epss();
        assertThat(epss.enabled()).isTrue();
        assertThat(epss.windowSeconds()).isEqualTo(120);
    }

    @Test
    @DisplayName("Base-URL und API-Key kommen NICHT aus dem Parameter-Store")
    void base_url_und_api_key_unveraendert() {
        given(resolver.resolveString("cvm.feed.nvd.base-url", ""))
                .willThrow(new IllegalStateException("darf nicht aufgerufen werden"));
        assertThat(effective.nvd().baseUrl()).isEqualTo("https://services.nvd.nist.gov");
        assertThat(effective.nvd().apiKey()).isEmpty();
    }
}
