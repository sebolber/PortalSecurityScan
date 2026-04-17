package com.ahs.cvm.integration.epss;

import com.ahs.cvm.integration.feed.CveEnrichment;
import com.ahs.cvm.integration.feed.FeedClientException;
import com.ahs.cvm.integration.feed.FeedMetrics;
import com.ahs.cvm.integration.feed.FeedProperties;
import com.ahs.cvm.integration.feed.FeedRateLimiter;
import com.ahs.cvm.integration.feed.FeedSource;
import com.ahs.cvm.integration.feed.VulnerabilityFeedClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adapter fuer den EPSS-Service von FIRST.org.
 *
 * <p>Score und Percentile werden in den Bereich [0,1] zurueckgeliefert
 * (4 Dezimalstellen, passt zu {@code numeric(5,4)} in der DB).
 */
@Component
public class EpssFeedClient implements VulnerabilityFeedClient {

    private final FeedProperties.FeedConfig config;
    private final FeedRateLimiter rateLimiter;
    private final FeedMetrics metrics;
    private final RestClient restClient;

    public EpssFeedClient(
            FeedProperties properties,
            FeedRateLimiter rateLimiter,
            FeedMetrics metrics) {
        this.config = properties.getEpss();
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .build();
    }

    @Override
    public FeedSource source() {
        return FeedSource.EPSS;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public Optional<CveEnrichment> fetch(String cveId) {
        if (!isEnabled()) return Optional.empty();
        rateLimiter.acquire(source());
        return metrics.messe(source(), () -> abfragen(cveId));
    }

    private Optional<CveEnrichment> abfragen(String cveId) {
        try {
            JsonNode body = restClient.get()
                    .uri(builder -> builder
                            .queryParam("cve", cveId)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null) return Optional.empty();
            JsonNode data = body.path("data");
            if (!data.isArray() || data.isEmpty()) {
                return Optional.empty();
            }
            JsonNode eintrag = data.get(0);
            BigDecimal score = eintrag.has("epss")
                    ? new BigDecimal(eintrag.path("epss").asText())
                            .setScale(4, RoundingMode.HALF_UP)
                    : null;
            BigDecimal percentile = eintrag.has("percentile")
                    ? new BigDecimal(eintrag.path("percentile").asText())
                            .setScale(4, RoundingMode.HALF_UP)
                    : null;
            return Optional.of(new CveEnrichment(
                    cveId, source(), null, null, null, null, null,
                    score, percentile, null, null));
        } catch (RuntimeException e) {
            throw new FeedClientException(source(), "EPSS-Abruf fehlgeschlagen", e);
        }
    }
}
