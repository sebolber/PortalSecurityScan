package com.ahs.cvm.integration.osv;

import com.ahs.cvm.application.cve.ComponentVulnerabilityLookup;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OSV-Implementierung (Iteration 33, CVM-77) des Ports
 * {@link ComponentVulnerabilityLookup}. Nutzt den Batch-Endpoint
 * {@code POST /v1/querybatch}, der bis zu 1000 Queries pro Request
 * akzeptiert und die Treffer in derselben Reihenfolge zurueckgibt.
 *
 * <p>Aus den Advisory-Eintraegen werden ausschliesslich CVE-IDs
 * extrahiert (entweder direkt, wenn das Advisory eine CVE-ID hat,
 * oder aus den Aliasen). Andere Advisory-Formate (GHSA-, GO-,
 * PYSEC-, RUSTSEC-IDs) werden nicht als Finding persistiert, weil
 * die CVE-Tabelle darauf nicht ausgelegt ist.
 */
@Component
@EnableConfigurationProperties(OsvProperties.class)
public class OsvComponentLookup implements ComponentVulnerabilityLookup {

    private static final Logger log = LoggerFactory.getLogger(
            OsvComponentLookup.class);

    private final OsvProperties props;
    private final RestClient restClient;

    public OsvComponentLookup(OsvProperties props, RestClient.Builder builder) {
        this.props = props;
        this.restClient = builder.baseUrl(props.getBaseUrl()).build();
    }

    /**
     * Einmalige Log-Zeile beim App-Start, damit Admins ohne den Weg
     * ueber {@code /actuator/env} sehen, ob OSV aktiv ist. Ohne
     * diese Meldung wuerde man im Fehlerfall "findingCount bleibt 0"
     * nicht unterscheiden koennen, ob der Flag aus ist oder OSV
     * nichts gefunden hat.
     */
    @PostConstruct
    void logStartupStatus() {
        if (isEnabled()) {
            log.info(
                    "OSV-Enrichment aktiv: base-url={}, batch-size={}, timeout-ms={}",
                    props.getBaseUrl(),
                    props.getBatchSize(),
                    props.getTimeoutMs());
        } else {
            log.info(
                    "OSV-Enrichment DEAKTIVIERT (cvm.enrichment.osv.enabled=false oder leere base-url). "
                    + "Setze CVM_OSV_ENABLED=true, um PURL->CVE-Matching zu nutzen.");
        }
    }

    @Override
    public boolean isEnabled() {
        return props.isEnabled()
                && props.getBaseUrl() != null
                && !props.getBaseUrl().isBlank();
    }

    @Override
    public Map<String, List<String>> findCveIdsForPurls(List<String> purls) {
        if (!isEnabled() || purls == null || purls.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> aggregate = new LinkedHashMap<>();
        int size = Math.max(1, props.getBatchSize());
        for (int i = 0; i < purls.size(); i += size) {
            List<String> chunk = purls.subList(
                    i, Math.min(i + size, purls.size()));
            Map<String, List<String>> partial = abfragen(chunk);
            aggregate.putAll(partial);
        }
        return aggregate;
    }

    private Map<String, List<String>> abfragen(List<String> purls) {
        try {
            Map<String, Object> body = neuRequestBody(purls);
            JsonNode response = restClient.post()
                    .uri("/v1/querybatch")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return Map.of();
            }
            JsonNode results = response.path("results");
            if (!results.isArray()) {
                return Map.of();
            }
            Map<String, List<String>> out = new LinkedHashMap<>();
            for (int idx = 0; idx < results.size() && idx < purls.size(); idx++) {
                List<String> cveIds = cveIdsAus(results.get(idx));
                if (!cveIds.isEmpty()) {
                    out.put(purls.get(idx), cveIds);
                }
            }
            return out;
        } catch (RuntimeException ex) {
            log.warn("OSV-Abfrage fehlgeschlagen ({} PURLs): {}",
                    purls.size(), ex.getMessage());
            return Map.of();
        }
    }

    private static Map<String, Object> neuRequestBody(List<String> purls) {
        List<Map<String, Object>> queries = new ArrayList<>(purls.size());
        for (String purl : purls) {
            queries.add(Map.of("package", Map.of("purl", purl)));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("queries", queries);
        return body;
    }

    private static List<String> cveIdsAus(JsonNode resultNode) {
        JsonNode vulns = resultNode.path("vulns");
        if (!vulns.isArray() || vulns.isEmpty()) {
            return List.of();
        }
        // LinkedHashSet: Reihenfolge fuer Tests stabil, Duplikate raus.
        LinkedHashSet<String> cves = new LinkedHashSet<>();
        for (JsonNode v : vulns) {
            String id = v.path("id").asText("");
            if (id.startsWith("CVE-")) {
                cves.add(id);
            }
            JsonNode aliases = v.path("aliases");
            if (aliases.isArray()) {
                for (JsonNode alias : aliases) {
                    String a = alias.asText("");
                    if (a.startsWith("CVE-")) {
                        cves.add(a);
                    }
                }
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(cves));
    }

}
