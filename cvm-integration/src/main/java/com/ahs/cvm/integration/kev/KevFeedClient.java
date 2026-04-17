package com.ahs.cvm.integration.kev;

import com.ahs.cvm.integration.feed.CveEnrichment;
import com.ahs.cvm.integration.feed.FeedClientException;
import com.ahs.cvm.integration.feed.FeedMetrics;
import com.ahs.cvm.integration.feed.FeedProperties;
import com.ahs.cvm.integration.feed.FeedRateLimiter;
import com.ahs.cvm.integration.feed.FeedSource;
import com.ahs.cvm.integration.feed.VulnerabilityFeedClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adapter fuer den CISA-KEV-Feed (JSON-Dump, taeglich).
 *
 * <p>Der Feed liefert eine Liste aller bekannten ausgenutzten
 * Schwachstellen mit {@code cveID} und {@code dateAdded}. Wir cachen den
 * aktuellen Dump in-memory, damit
 * {@link #fetch(String)} ohne erneuten Netzwerk-Round-Trip auskommt.
 */
@Component
public class KevFeedClient implements VulnerabilityFeedClient {

    private static final Logger log = LoggerFactory.getLogger(KevFeedClient.class);

    private final FeedProperties.FeedConfig config;
    private final FeedRateLimiter rateLimiter;
    private final FeedMetrics metrics;
    private final RestClient restClient;

    private volatile Map<String, CveEnrichment> cache = Map.of();

    public KevFeedClient(
            FeedProperties properties,
            FeedRateLimiter rateLimiter,
            FeedMetrics metrics) {
        this.config = properties.getKev();
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        this.restClient = RestClient.create();
    }

    @Override
    public FeedSource source() {
        return FeedSource.KEV;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public Optional<CveEnrichment> fetch(String cveId) {
        if (!isEnabled()) return Optional.empty();
        return Optional.ofNullable(cache.get(cveId));
    }

    @Override
    public List<CveEnrichment> fetchAll() {
        if (!isEnabled()) return List.of();
        rateLimiter.acquire(source());
        return metrics.messe(source(), this::lade);
    }

    private List<CveEnrichment> lade() {
        try {
            JsonNode dump = restClient.get()
                    .uri(config.getBaseUrl())
                    .retrieve()
                    .body(JsonNode.class);
            List<CveEnrichment> ergebnisse = new ArrayList<>();
            Map<String, CveEnrichment> neuesCache = new HashMap<>();
            if (dump != null && dump.path("vulnerabilities").isArray()) {
                for (JsonNode v : dump.path("vulnerabilities")) {
                    String cveId = v.path("cveID").asText();
                    if (cveId.isBlank()) continue;
                    Instant dateAdded = null;
                    if (v.has("dateAdded")) {
                        dateAdded = LocalDate.parse(v.path("dateAdded").asText())
                                .atStartOfDay()
                                .toInstant(ZoneOffset.UTC);
                    }
                    CveEnrichment enrichment = new CveEnrichment(
                            cveId, source(), null, null, null,
                            Boolean.TRUE, dateAdded, null, null, null, null);
                    ergebnisse.add(enrichment);
                    neuesCache.put(cveId, enrichment);
                }
            }
            cache = neuesCache;
            log.info("KEV-Dump geladen: {} Eintraege", ergebnisse.size());
            return ergebnisse;
        } catch (RuntimeException e) {
            throw new FeedClientException(source(), "KEV-Abruf fehlgeschlagen", e);
        }
    }
}
