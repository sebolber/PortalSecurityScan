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
 * <p>Iteration 78 (CVM-315): beruecksichtigt den optionalen
 * {@code affected[*].versions}-Array. Ist er nicht leer, matcht die
 * Advisory nur Queries, deren PURL-Version in der Liste vorkommt.
 * Bei leerer/fehlender Liste bleibt das bisherige "match all"-
 * Verhalten erhalten (konservativer Default fuer
 * air-gapped-Faelle, in denen die Dump-Qualitaet schwankt).
 *
 * <p>Die Klasse bleibt eigenstaendig (keine Spring-Abhaengigkeit),
 * damit sie aus Unit-Tests und einem optionalen CLI-Refresh-Job
 * direkt instanziiert werden kann.
 */
public final class OsvJsonlMirror {

    private static final Logger log = LoggerFactory.getLogger(OsvJsonlMirror.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path jsonlFile;
    private final AtomicReference<Map<String, List<AdvisoryEntry>>> index =
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
        Map<String, List<AdvisoryEntry>> neu = baueIndex(jsonlFile);
        index.set(neu);
        log.info("OSV-Mirror geladen: {} Basis-PURLs aus {}",
                neu.size(), jsonlFile);
    }

    /** Aktuelle Anzahl unterschiedlicher Basis-PURLs. */
    public int size() {
        return index.get().size();
    }

    public Map<String, List<String>> findCveIdsForPurls(List<String> purls) {
        if (purls == null || purls.isEmpty()) {
            return Map.of();
        }
        Map<String, List<AdvisoryEntry>> snapshot = index.get();
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (String purl : purls) {
            PurlParts parts = PurlParts.parse(purl);
            List<AdvisoryEntry> entries = snapshot.get(parts.base());
            if (entries == null || entries.isEmpty()) {
                continue;
            }
            LinkedHashSet<String> treffer = new LinkedHashSet<>();
            for (AdvisoryEntry e : entries) {
                if (e.matches(parts.version())) {
                    treffer.addAll(e.cveIds());
                }
            }
            if (!treffer.isEmpty()) {
                out.put(purl, List.copyOf(treffer));
            }
        }
        return out;
    }

    private static Map<String, List<AdvisoryEntry>> baueIndex(Path jsonlFile) {
        if (jsonlFile == null || !Files.isRegularFile(jsonlFile)) {
            log.warn("OSV-Mirror-File nicht gefunden: {}", jsonlFile);
            return Map.of();
        }
        Map<String, List<AdvisoryEntry>> acc = new LinkedHashMap<>();
        try (Stream<String> lines = Files.lines(jsonlFile)) {
            lines.forEach(line -> verarbeiteZeile(line, acc));
        } catch (IOException ex) {
            log.warn("OSV-Mirror-File {} nicht lesbar: {}", jsonlFile, ex.getMessage());
            return Map.of();
        }
        Map<String, List<AdvisoryEntry>> out = new LinkedHashMap<>();
        acc.forEach((base, list) -> out.put(base, List.copyOf(list)));
        return Collections.unmodifiableMap(out);
    }

    private static void verarbeiteZeile(
            String line, Map<String, List<AdvisoryEntry>> acc) {
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
        JsonNode affected = advisory.path("affected");
        if (!affected.isArray()) {
            return;
        }
        for (JsonNode entry : affected) {
            String purl = entry.path("package").path("purl").asText("");
            if (purl.isBlank()) {
                continue;
            }
            Set<String> versions = versionenVonAffected(entry);
            PurlParts parts = PurlParts.parse(purl);
            AdvisoryEntry ae = new AdvisoryEntry(List.copyOf(cveIds), versions);
            acc.computeIfAbsent(parts.base(), k -> new ArrayList<>()).add(ae);
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

    private static Set<String> versionenVonAffected(JsonNode affectedEntry) {
        JsonNode versions = affectedEntry.path("versions");
        if (!versions.isArray() || versions.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (JsonNode v : versions) {
            String s = v.asText("");
            if (!s.isBlank()) {
                out.add(s);
            }
        }
        return Collections.unmodifiableSet(out);
    }

    /**
     * Paket-PURL (ohne Version) plus der getrennte Versions-Anteil.
     * Iteration 78 (CVM-315).
     */
    record PurlParts(String base, String version) {
        static PurlParts parse(String purl) {
            if (purl == null || purl.isBlank()) {
                return new PurlParts("", null);
            }
            // PURLs haben das Format pkg:type/namespace/name@version?qualifiers.
            // Wir trennen am letzten '@' vor einem optionalen '?'. Einfacher
            // und korrekt genug: erstes '@' wird als Trenner genommen, '?'
            // wird separat abgeschnitten.
            String qualifiersStripped = purl;
            int queryIndex = purl.indexOf('?');
            if (queryIndex >= 0) {
                qualifiersStripped = purl.substring(0, queryIndex);
            }
            int at = qualifiersStripped.indexOf('@');
            if (at < 0) {
                return new PurlParts(qualifiersStripped, null);
            }
            return new PurlParts(
                    qualifiersStripped.substring(0, at),
                    qualifiersStripped.substring(at + 1));
        }
    }

    /**
     * Eine einzelne Advisory-PURL-Zuordnung im Index.
     * {@code versions} leer => matcht alle Versions; gesetzt => nur
     * Queries, deren PURL-Version in der Menge liegt.
     */
    record AdvisoryEntry(List<String> cveIds, Set<String> versions) {
        boolean matches(String queryVersion) {
            if (versions == null || versions.isEmpty()) {
                return true;
            }
            if (queryVersion == null || queryVersion.isBlank()) {
                // Query ohne Version -> konservativ matchen, damit
                // Scans ohne Version-Info keine Findings verlieren.
                return true;
            }
            return versions.contains(queryVersion);
        }
    }
}
