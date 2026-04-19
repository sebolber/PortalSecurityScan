package com.ahs.cvm.integration.feed;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import org.springframework.stereotype.Component;

/**
 * Liest zur Laufzeit den System-Parameter-Store fuer die vier Feed-Quellen
 * NVD, GHSA, KEV, EPSS und faellt auf die beim Boot geladenen
 * {@link FeedProperties} zurueck.
 *
 * <p>{@code base-url} und {@code api-key} bleiben bewusst ausgeklammert
 * (Nicht-migrieren-Liste in {@code docs/20260419/offene-punkte.md}): die
 * {@code base-url} wird in den {@code RestClient.Builder} hinein-
 * zementiert; der {@code api-key} wird in Iteration 45 via AES-GCM
 * abgehandelt.
 */
@Component
public class FeedEffectiveProperties {

    private final FeedProperties props;
    private final SystemParameterResolver resolver;

    public FeedEffectiveProperties(FeedProperties props, SystemParameterResolver resolver) {
        this.props = props;
        this.resolver = resolver;
    }

    public EffectiveFeed nvd() {
        return forSlug("nvd", props.getNvd());
    }

    public EffectiveFeed ghsa() {
        return forSlug("ghsa", props.getGhsa());
    }

    public EffectiveFeed kev() {
        return forSlug("kev", props.getKev());
    }

    public EffectiveFeed epss() {
        return forSlug("epss", props.getEpss());
    }

    private EffectiveFeed forSlug(String slug, FeedProperties.FeedConfig config) {
        boolean enabled = resolver.resolveBoolean(
                "cvm.feed." + slug + ".enabled", config.isEnabled());
        int requestsPerWindow = resolver.resolveInt(
                "cvm.feed." + slug + ".requests-per-window",
                config.getRequestsPerWindow());
        int windowSeconds = resolver.resolveInt(
                "cvm.feed." + slug + ".window-seconds",
                config.getWindowSeconds());
        return new EffectiveFeed(
                config.getBaseUrl(),
                config.getApiKey(),
                enabled,
                requestsPerWindow,
                windowSeconds);
    }

    public record EffectiveFeed(
            String baseUrl,
            String apiKey,
            boolean enabled,
            int requestsPerWindow,
            int windowSeconds) {}
}
