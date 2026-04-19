package com.ahs.cvm.integration.osv;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import org.springframework.stereotype.Component;

/**
 * Liest zur Laufzeit den System-Parameter-Store und faellt auf die beim Boot
 * geladenen {@link OsvProperties} zurueck.
 *
 * <p>Die {@code base-url} bleibt absichtlich in {@code application.yaml}
 * (Nicht-migrieren-Liste aus {@code docs/20260419/offene-punkte.md}), da sie
 * in den {@link org.springframework.web.client.RestClient}-Builder hinein-
 * zementiert wird.
 */
@Component
public class OsvEffectiveProperties {

    private final OsvProperties props;
    private final SystemParameterResolver resolver;

    public OsvEffectiveProperties(OsvProperties props, SystemParameterResolver resolver) {
        this.props = props;
        this.resolver = resolver;
    }

    public boolean isEnabled() {
        return resolver.resolveBoolean("cvm.enrichment.osv.enabled", props.isEnabled());
    }

    public String getBaseUrl() {
        return props.getBaseUrl();
    }

    public int getBatchSize() {
        return resolver.resolveInt("cvm.enrichment.osv.batch-size", props.getBatchSize());
    }

    public int getTimeoutMs() {
        return resolver.resolveInt("cvm.enrichment.osv.timeout-ms", props.getTimeoutMs());
    }

    public boolean isRetryOn429() {
        return resolver.resolveBoolean("cvm.enrichment.osv.retry-on-429", props.isRetryOn429());
    }

    public int getMaxRetryAfterSeconds() {
        return resolver.resolveInt(
                "cvm.enrichment.osv.max-retry-after-seconds",
                props.getMaxRetryAfterSeconds());
    }
}
