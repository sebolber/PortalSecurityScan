package com.ahs.cvm.integration.ghsa;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
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
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final String PARAM_API_KEY = "cvm.feed.ghsa.api-key";

    private final FeedProperties.FeedConfig config;
    private final FeedRateLimiter rateLimiter;
    private final FeedMetrics metrics;
    private final RestClient restClient;
    private final Optional<SystemParameterResolver> parameterResolver;

    @Autowired
    public GhsaFeedClient(
            FeedProperties properties,
            FeedRateLimiter rateLimiter,
            FeedMetrics metrics,
            Optional<SystemParameterResolver> parameterResolver) {
        this.config = properties.getGhsa();
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        this.parameterResolver = parameterResolver == null
                ? Optional.empty()
                : parameterResolver;
        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .build();
    }

    /** Konstruktor fuer Bestands-Tests ohne Parameter-Resolver. */
    public GhsaFeedClient(
            FeedProperties properties,
            FeedRateLimiter rateLimiter,
            FeedMetrics metrics) {
        this(properties, rateLimiter, metrics, Optional.empty());
    }

    @Override
    public FeedSource source() {
        return FeedSource.GHSA;
    }

    @Override
    public boolean isEnabled() {
        String token = resolveApiKey();
        return config.isEnabled() && token != null && !token.isBlank();
    }

    /**
     * Iteration 67 (CVM-304): Bearer-Token pro Call aus dem
     * System-Parameter-Store lesen. Live-Wechsel des Tokens greift
     * ohne Neustart.
     */
    private String resolveApiKey() {
        return parameterResolver
                .flatMap(r -> r.resolve(PARAM_API_KEY))
                .filter(v -> !v.isBlank())
                .orElse(config.getApiKey());
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
        String token = resolveApiKey();
        try {
            JsonNode body = restClient.post()
                    .header("Authorization", "Bearer " + token)
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
