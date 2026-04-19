package com.ahs.cvm.integration.osv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File-basierter OSV-Mirror fuer air-gapped Setups (Iteration 72,
 * CVM-309). Erwartet ein JSONL-File - genau eine OSV-Advisory pro
 * Zeile, Format wie in den OSV.dev-Exports.
 *
 * <p>Die Klasse ist bewusst eigenstaendig (keine Spring-
 * Abhaengigkeit), damit sie aus Unit-Tests und einem optionalen
 * CLI-Refresh-Job direkt instanziiert werden kann. Die
 * {@link OsvJsonlMirrorLookup}-Komponente verpackt sie als
 * {@link com.ahs.cvm.application.cve.ComponentVulnerabilityLookup}
 * in den Spring-Context.
 *
 * <p>Aktuell kein Versionsbereich-Matching: ein Advisory, das einen
 * PURL erwaehnt, liefert fuer diesen PURL alle referenzierten CVE-
 * Aliase. Das ist fuer den air-gapped-MVP genug; genauere Filter
 * sind ein Follow-up.
 */
public final class OsvJsonlMirror {

    private static final Logger log = LoggerFactory.getLogger(OsvJsonlMirror.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path jsonlFile;
    private final AtomicReference<Map<String, List<String>>> index =
            new AtomicReference<>(Map.of());

    public OsvJsonlMirror(Path jsonlFile) {
        this.jsonlFile = jsonlFile;
    }

    /**
     * Liest die Datei neu ein und aktualisiert den Index atomar.
     * Darf zur Laufzeit aufgerufen werden (z.B. aus einem
     * Refresh-Endpunkt); bis zum Abschluss liefert
     * {@link #findCveIdsForPurls(List)} weiterhin die alten Daten.
     */
    public void reload() {
        Map<String, List<String>> neu = baueIndex(jsonlFile);
        index.set(neu);
        log.info("OSV-Mirror geladen: {} PURL-Eintraege aus {}",
                neu.size(), jsonlFile);
    }

    /** Aktuelle Index-Groesse, vor allem fuer Logs / Health-Checks. */
    public int size() {
        return index.get().size();
    }

    public Map<String, List<String>> findCveIdsForPurls(List<String> purls) {
        if (purls == null || purls.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> snapshot = index.get();
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (String purl : purls) {
            List<String> cves = snapshot.get(purl);
            if (cves != null && !cves.isEmpty()) {
                out.put(purl, cves);
            }
        }
        return out;
    }

    private static Map<String, List<String>> baueIndex(Path jsonlFile) {
        if (jsonlFile == null || !Files.isRegularFile(jsonlFile)) {
            log.warn("OSV-Mirror-File nicht gefunden: {}", jsonlFile);
            return Map.of();
        }
        Map<String, LinkedHashSet<String>> acc = new LinkedHashMap<>();
        try (Stream<String> lines = Files.lines(jsonlFile)) {
            lines.forEach(line -> verarbeiteZeile(line, acc));
        } catch (IOException ex) {
            log.warn("OSV-Mirror-File {} nicht lesbar: {}", jsonlFile, ex.getMessage());
            return Map.of();
        }
        Map<String, List<String>> out = new LinkedHashMap<>();
        acc.forEach((purl, cves) -> out.put(purl, List.copyOf(cves)));
        return Collections.unmodifiableMap(out);
    }

    private static void verarbeiteZeile(
            String line, Map<String, LinkedHashSet<String>> acc) {
        if (line == null || line.isBlank()) {
            return;
        }
        JsonNode advisory;
        try {
            advisory = MAPPER.readTree(line);
        } catch (Exception ex) {
            log.debug("Ignoriere defekte OSV-Zeile: {}", ex.getMessage());
            return;
        }
        Set<String> cveIds = cveIdsVonAdvisory(advisory);
        if (cveIds.isEmpty()) {
            return;
        }
        List<String> purls = purlsVonAdvisory(advisory);
        for (String purl : purls) {
            acc.computeIfAbsent(purl, k -> new LinkedHashSet<>()).addAll(cveIds);
        }
    }

    private static Set<String> cveIdsVonAdvisory(JsonNode advisory) {
        LinkedHashSet<String> cves = new LinkedHashSet<>();
        String id = advisory.path("id").asText("");
        if (id.startsWith("CVE-")) {
            cves.add(id);
        }
        JsonNode aliases = advisory.path("aliases");
        if (aliases.isArray()) {
            for (JsonNode alias : aliases) {
                String a = alias.asText("");
                if (a.startsWith("CVE-")) {
                    cves.add(a);
                }
            }
        }
        return cves;
    }

    private static List<String> purlsVonAdvisory(JsonNode advisory) {
        List<String> purls = new ArrayList<>();
        JsonNode affected = advisory.path("affected");
        if (!affected.isArray()) {
            return purls;
        }
        for (JsonNode entry : affected) {
            String purl = entry.path("package").path("purl").asText("");
            if (!purl.isBlank()) {
                purls.add(purl);
            }
        }
        return purls;
    }
}
