package com.ahs.cvm.integration.nvd;

import com.ahs.cvm.integration.feed.CveEnrichment;
import com.ahs.cvm.integration.feed.FeedClientException;
import com.ahs.cvm.integration.feed.FeedMetrics;
import com.ahs.cvm.integration.feed.FeedProperties;
import com.ahs.cvm.integration.feed.FeedRateLimiter;
import com.ahs.cvm.integration.feed.FeedSource;
import com.ahs.cvm.integration.feed.VulnerabilityFeedClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adapter fuer den NVD-REST-API 2.0-Endpunkt
 * {@code /rest/json/cves/2.0?cveId=...}. Extrahiert die fuer CVM
 * relevanten Felder (Beschreibung, CVSS-3.1-Vektor, Score, CWE-Liste,
 * Referenzen).
 */
@Component
public class NvdFeedClient implements VulnerabilityFeedClient {

    private static final Logger log = LoggerFactory.getLogger(NvdFeedClient.class);

    private final FeedProperties.FeedConfig config;
    private final FeedRateLimiter rateLimiter;
    private final FeedMetrics metrics;
    private final RestClient restClient;

    public NvdFeedClient(
            FeedProperties properties,
            FeedRateLimiter rateLimiter,
            FeedMetrics metrics) {
        this.config = properties.getNvd();
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .build();
    }

    @Override
    public FeedSource source() {
        return FeedSource.NVD;
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
                            .path("/rest/json/cves/2.0")
                            .queryParam("cveId", cveId)
                            .build())
                    .headers(h -> {
                        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
                            h.add("apiKey", config.getApiKey());
                        }
                    })
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null) {
                return Optional.empty();
            }
            JsonNode vulnerabilities = body.path("vulnerabilities");
            if (!vulnerabilities.isArray() || vulnerabilities.isEmpty()) {
                return Optional.empty();
            }
            JsonNode cve = vulnerabilities.get(0).path("cve");
            return Optional.of(zuEnrichment(cveId, cve));
        } catch (RuntimeException e) {
            log.warn("NVD-Abruf fuer {} fehlgeschlagen: {}", cveId, e.getMessage());
            throw new FeedClientException(source(), "NVD-Abruf fehlgeschlagen", e);
        }
    }

    private CveEnrichment zuEnrichment(String cveId, JsonNode cve) {
        String beschreibung = null;
        for (JsonNode d : cve.path("descriptions")) {
            if ("en".equals(d.path("lang").asText())) {
                beschreibung = d.path("value").asText();
                break;
            }
        }
        BigDecimal score = null;
        String vektor = null;
        JsonNode metrics31 = cve.path("metrics").path("cvssMetricV31");
        if (metrics31.isArray() && !metrics31.isEmpty()) {
            JsonNode cvss = metrics31.get(0).path("cvssData");
            if (cvss.has("baseScore")) {
                score = BigDecimal.valueOf(cvss.path("baseScore").asDouble());
            }
            if (cvss.has("vectorString")) {
                vektor = cvss.path("vectorString").asText();
            }
        }
        List<String> cwes = new ArrayList<>();
        for (JsonNode weakness : cve.path("weaknesses")) {
            for (JsonNode desc : weakness.path("description")) {
                String value = desc.path("value").asText();
                if (value.startsWith("CWE-")) {
                    cwes.add(value);
                }
            }
        }
        return new CveEnrichment(
                cveId,
                source(),
                beschreibung,
                score,
                vektor,
                null,
                null,
                null,
                null,
                cwes.isEmpty() ? null : cwes,
                null);
    }
}
