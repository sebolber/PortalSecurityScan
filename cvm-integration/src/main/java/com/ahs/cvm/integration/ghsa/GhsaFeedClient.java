package com.ahs.cvm.integration.ghsa;

import com.ahs.cvm.integration.feed.CveEnrichment;
import com.ahs.cvm.integration.feed.FeedClientException;
import com.ahs.cvm.integration.feed.FeedMetrics;
import com.ahs.cvm.integration.feed.FeedProperties;
import com.ahs.cvm.integration.feed.FeedRateLimiter;
import com.ahs.cvm.integration.feed.FeedSource;
import com.ahs.cvm.integration.feed.VulnerabilityFeedClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adapter fuer den GitHub-Advisory-Feed (GHSA) ueber GraphQL.
 *
 * <p>Standardmaessig deaktiviert (braucht GitHub-Token). Die Implementierung
 * legt Struktur und Testanbindung fest; eine ausfuehrliche GraphQL-Query
 * wird in einer Folge-Iteration ergaenzt.
 */
@Component
public class GhsaFeedClient implements VulnerabilityFeedClient {

    private final FeedProperties.FeedConfig config;
    private final FeedRateLimiter rateLimiter;
    private final FeedMetrics metrics;
    private final RestClient restClient;

    public GhsaFeedClient(
            FeedProperties properties,
            FeedRateLimiter rateLimiter,
            FeedMetrics metrics) {
        this.config = properties.getGhsa();
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .build();
    }

    @Override
    public FeedSource source() {
        return FeedSource.GHSA;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled()
                && config.getApiKey() != null
                && !config.getApiKey().isBlank();
    }

    @Override
    public Optional<CveEnrichment> fetch(String cveId) {
        if (!isEnabled()) return Optional.empty();
        rateLimiter.acquire(source());
        return metrics.messe(source(), () -> abfragen(cveId));
    }

    private Optional<CveEnrichment> abfragen(String cveId) {
        String query = """
                { "query": "query($cve:String!){ securityAdvisories(identifier:{type:CVE,value:$cve}, first:1){nodes{ghsaId summary references{url}}}}", "variables": { "cve": "%s" } }
                """.formatted(cveId);
        try {
            JsonNode body = restClient.post()
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(query)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null) return Optional.empty();
            JsonNode nodes = body.path("data").path("securityAdvisories").path("nodes");
            if (!nodes.isArray() || nodes.isEmpty()) return Optional.empty();
            JsonNode advisory = nodes.get(0);

            List<Map<String, Object>> advisories = new ArrayList<>();
            Map<String, Object> advisoryMap = new HashMap<>();
            advisoryMap.put("ghsaId", advisory.path("ghsaId").asText());
            advisoryMap.put("summary", advisory.path("summary").asText());
            List<String> urls = new ArrayList<>();
            for (JsonNode ref : advisory.path("references")) {
                urls.add(ref.path("url").asText());
            }
            advisoryMap.put("references", urls);
            advisories.add(advisoryMap);

            return Optional.of(new CveEnrichment(
                    cveId, source(), advisory.path("summary").asText(),
                    null, null, null, null, null, null, null, advisories));
        } catch (RuntimeException e) {
            throw new FeedClientException(source(), "GHSA-Abruf fehlgeschlagen", e);
        }
    }
}
