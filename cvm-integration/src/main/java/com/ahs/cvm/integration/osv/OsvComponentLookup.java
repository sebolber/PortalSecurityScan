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
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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
    private final Sleeper sleeper;

    public OsvComponentLookup(OsvProperties props, RestClient.Builder builder) {
        this(props, builder, Sleeper.defaultSleeper());
    }

    /** Konstruktor fuer Tests, damit wir nicht echte Sekunden warten muessen. */
    OsvComponentLookup(OsvProperties props, RestClient.Builder builder, Sleeper sleeper) {
        this.props = props;
        this.restClient = builder.baseUrl(props.getBaseUrl()).build();
        this.sleeper = sleeper;
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
                    "OSV-Enrichment aktiv: base-url={}, batch-size={}, timeout-ms={}, retryOn429={}, maxRetryAfterSeconds={}",
                    props.getBaseUrl(),
                    props.getBatchSize(),
                    props.getTimeoutMs(),
                    props.isRetryOn429(),
                    props.getMaxRetryAfterSeconds());
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
            JsonNode response = mitRetryAuf429(
                    "querybatch",
                    () -> restClient.post()
                            .uri("/v1/querybatch")
                            .header("Content-Type", "application/json")
                            .body(body)
                            .retrieve()
                            .body(JsonNode.class));
            if (response == null) {
                return Map.of();
            }
            JsonNode results = response.path("results");
            if (!results.isArray()) {
                return Map.of();
            }
            // Cache fuer den Detail-Fallback: OSV liefert im Batch nur
            // Advisory-IDs (meist GHSA), die Aliase nur beim Detail-Call.
            // Pro Batch-Aufruf wird jede Advisory-ID nur einmal aufgeloest.
            Map<String, List<String>> aliasCache = new HashMap<>();
            Map<String, List<String>> out = new LinkedHashMap<>();
            for (int idx = 0; idx < results.size() && idx < purls.size(); idx++) {
                List<String> cveIds = cveIdsAus(results.get(idx), aliasCache);
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

    private List<String> cveIdsAus(
            JsonNode resultNode, Map<String, List<String>> aliasCache) {
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
                continue;
            }
            // Aliase aus der Batch-Response bevorzugen (spart Request).
            boolean aliasTreffer = false;
            JsonNode aliases = v.path("aliases");
            if (aliases.isArray()) {
                for (JsonNode alias : aliases) {
                    String a = alias.asText("");
                    if (a.startsWith("CVE-")) {
                        cves.add(a);
                        aliasTreffer = true;
                    }
                }
            }
            // Fallback: Detail-Call. OSV liefert im Batch kaum Aliasen;
            // erst /v1/vulns/<id> gibt die CVE-Verknuepfung.
            if (!aliasTreffer && !id.isEmpty()) {
                cves.addAll(aliasCache.computeIfAbsent(
                        id, this::resolveAliasesFromDetail));
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(cves));
    }

    /**
     * Loest die CVE-Aliase fuer eine Advisory-ID via
     * {@code GET /v1/vulns/<id>} auf. Rueckgabe ist leer bei Netz-/
     * Protokollfehlern, damit ein Einzelausfall den gesamten Scan-Run
     * nicht blockiert.
     */
    /**
     * Fuehrt {@code call} aus und retried einmalig auf HTTP-429, wenn
     * {@link OsvProperties#isRetryOn429()} gesetzt ist. Die Wartezeit
     * wird aus dem {@code Retry-After}-Header gelesen (nur Sekunden-
     * Form); {@link OsvProperties#getMaxRetryAfterSeconds()} deckelt
     * sie, damit der Server uns nicht 15 Minuten blockieren kann.
     */
    private <T> T mitRetryAuf429(String label, Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException.TooManyRequests tooMany) {
            if (!props.isRetryOn429()) {
                log.warn("OSV-{} lieferte 429 und Retry ist deaktiviert.", label);
                throw tooMany;
            }
            int wait = retryAfterSekunden(tooMany);
            log.info("OSV-{} lieferte 429; einmalig nach {} s wiederholen.",
                    label, wait);
            sleeper.sleepSeconds(wait);
            return call.get();
        }
    }

    private int retryAfterSekunden(HttpClientErrorException ex) {
        String header = ex.getResponseHeaders() == null
                ? null
                : ex.getResponseHeaders().getFirst("Retry-After");
        int wait = 1;
        if (header != null) {
            try {
                wait = Integer.parseInt(header.trim());
            } catch (NumberFormatException ignored) {
                // Retry-After kann auch ein HTTP-Datum sein; wir
                // behandeln nur die Sekunden-Variante. Andernfalls
                // Fallback auf 1 s.
                wait = 1;
            }
        }
        if (wait < 0) {
            wait = 0;
        }
        int max = Math.max(0, props.getMaxRetryAfterSeconds());
        return Math.min(wait, max);
    }

    /**
     * Test-Hook: erlaubt das Durchreichen eines synchronen Sleepers.
     */
    @FunctionalInterface
    interface Sleeper {
        void sleepSeconds(int seconds);

        static Sleeper defaultSleeper() {
            return seconds -> {
                if (seconds <= 0) {
                    return;
                }
                try {
                    Thread.sleep(seconds * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            };
        }
    }

    private List<String> resolveAliasesFromDetail(String advisoryId) {
        try {
            JsonNode detail = mitRetryAuf429(
                    "vulns/" + advisoryId,
                    () -> restClient.get()
                            .uri("/v1/vulns/{id}", advisoryId)
                            .retrieve()
                            .body(JsonNode.class));
            if (detail == null) {
                return List.of();
            }
            LinkedHashSet<String> cves = new LinkedHashSet<>();
            JsonNode aliases = detail.path("aliases");
            if (aliases.isArray()) {
                for (JsonNode alias : aliases) {
                    String a = alias.asText("");
                    if (a.startsWith("CVE-")) {
                        cves.add(a);
                    }
                }
            }
            String detailId = detail.path("id").asText("");
            if (detailId.startsWith("CVE-")) {
                cves.add(detailId);
            }
            return List.copyOf(cves);
        } catch (RuntimeException ex) {
            log.warn("OSV-Detail-Abfrage fuer {} fehlgeschlagen: {}",
                    advisoryId, ex.getMessage());
            return List.of();
        }
    }

}
