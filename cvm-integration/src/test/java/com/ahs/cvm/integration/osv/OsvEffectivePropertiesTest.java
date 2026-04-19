package com.ahs.cvm.integration.osv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OsvEffectivePropertiesTest {

    private OsvProperties props;
    private SystemParameterResolver resolver;
    private OsvEffectiveProperties effective;

    @BeforeEach
    void setUp() {
        props = new OsvProperties();
        props.setEnabled(false);
        props.setBaseUrl("https://api.osv.dev");
        props.setBatchSize(500);
        props.setTimeoutMs(15_000);
        props.setRetryOn429(true);
        props.setMaxRetryAfterSeconds(30);

        resolver = mock(SystemParameterResolver.class);
        effective = new OsvEffectiveProperties(props, resolver);
    }

    @Test
    @DisplayName("Ohne DB-Override werden die Werte aus OsvProperties zurueckgegeben")
    void fallback_auf_properties() {
        given(resolver.resolveBoolean("cvm.enrichment.osv.enabled", false)).willReturn(false);
        given(resolver.resolveInt("cvm.enrichment.osv.batch-size", 500)).willReturn(500);
        given(resolver.resolveInt("cvm.enrichment.osv.timeout-ms", 15_000)).willReturn(15_000);
        given(resolver.resolveBoolean("cvm.enrichment.osv.retry-on-429", true)).willReturn(true);
        given(resolver.resolveInt("cvm.enrichment.osv.max-retry-after-seconds", 30)).willReturn(30);

        assertThat(effective.isEnabled()).isFalse();
        assertThat(effective.getBatchSize()).isEqualTo(500);
        assertThat(effective.getTimeoutMs()).isEqualTo(15_000);
        assertThat(effective.isRetryOn429()).isTrue();
        assertThat(effective.getMaxRetryAfterSeconds()).isEqualTo(30);
        assertThat(effective.getBaseUrl()).isEqualTo("https://api.osv.dev");
    }

    @Test
    @DisplayName("DB-Override dreht isEnabled ohne Neustart um und erhoeht Batch-Size")
    void db_override() {
        given(resolver.resolveBoolean("cvm.enrichment.osv.enabled", false)).willReturn(true);
        given(resolver.resolveInt("cvm.enrichment.osv.batch-size", 500)).willReturn(900);
        given(resolver.resolveInt("cvm.enrichment.osv.timeout-ms", 15_000)).willReturn(5_000);
        given(resolver.resolveBoolean("cvm.enrichment.osv.retry-on-429", true)).willReturn(false);
        given(resolver.resolveInt("cvm.enrichment.osv.max-retry-after-seconds", 30)).willReturn(10);

        assertThat(effective.isEnabled()).isTrue();
        assertThat(effective.getBatchSize()).isEqualTo(900);
        assertThat(effective.getTimeoutMs()).isEqualTo(5_000);
        assertThat(effective.isRetryOn429()).isFalse();
        assertThat(effective.getMaxRetryAfterSeconds()).isEqualTo(10);
    }

    @Test
    @DisplayName("Base-URL wird NICHT aus dem Parameter-Store gelesen (Nicht-migrieren-Liste)")
    void base_url_kommt_nur_aus_properties() {
        assertThat(effective.getBaseUrl()).isEqualTo(props.getBaseUrl());
    }
}
