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
            SemverRange range = rangeVonAffected(entry);
            PurlParts parts = PurlParts.parse(purl);
            AdvisoryEntry ae = new AdvisoryEntry(
                    List.copyOf(cveIds), versions, range);
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
     * Iteration 79 (CVM-316): liest ein optionales
     * {@code ranges[0].events}-Paar mit {@code introduced} und
     * {@code fixed} - nur, wenn beide Versionen numerisch 3-
     * teilig sind (semver-Style). Komplizierte Bereiche werden
     * ignoriert (fallen auf "alle Versionen" zurueck).
     */
    private static SemverRange rangeVonAffected(JsonNode affectedEntry) {
        JsonNode ranges = affectedEntry.path("ranges");
        if (!ranges.isArray() || ranges.isEmpty()) {
            return SemverRange.ALL;
        }
        JsonNode events = ranges.get(0).path("events");
        if (!events.isArray() || events.isEmpty()) {
            return SemverRange.ALL;
        }
        String introduced = null;
        String fixed = null;
        for (JsonNode ev : events) {
            if (ev.has("introduced")) {
                introduced = ev.get("introduced").asText("");
            }
            if (ev.has("fixed")) {
                fixed = ev.get("fixed").asText("");
            }
        }
        if (introduced == null || fixed == null) {
            return SemverRange.ALL;
        }
        int[] introducedParts = SemverRange.parse(introduced);
        int[] fixedParts = SemverRange.parse(fixed);
        if (introducedParts == null || fixedParts == null) {
            return SemverRange.ALL;
        }
        return new SemverRange(introducedParts, fixedParts);
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
     * {@code versions} leer + {@code range.all()} => matcht alle
     * Versionen; {@code versions} nicht-leer => exakte Liste;
     * {@code range} nicht-ALL => semver-3-Tupel introduced &lt;=
     * Query &lt; fixed.
     */
    record AdvisoryEntry(
            List<String> cveIds, Set<String> versions, SemverRange range) {

        AdvisoryEntry(List<String> cveIds, Set<String> versions) {
            this(cveIds, versions, SemverRange.ALL);
        }

        boolean matches(String queryVersion) {
            if ((versions == null || versions.isEmpty())
                    && (range == null || range.all())) {
                return true;
            }
            if (queryVersion == null || queryVersion.isBlank()) {
                // Query ohne Version -> konservativ matchen, damit
                // Scans ohne Version-Info keine Findings verlieren.
                return true;
            }
            if (versions != null && versions.contains(queryVersion)) {
                return true;
            }
            return range != null && !range.all() && range.contains(queryVersion);
        }
    }

    /**
     * Iteration 79 (CVM-316): sehr schlanker semver-3-Tupel-
     * Bereichsvergleich, nur fuer numerische {@code X.Y.Z}-
     * Versionen. Keine Praefix-/Qualifier-Unterstuetzung
     * (z.B. {@code 1.0.0-rc1} -> "nicht-parseable" -> Range.ALL).
     */
    record SemverRange(int[] introduced, int[] fixed) {
        static final SemverRange ALL = new SemverRange(null, null);

        boolean all() {
            return introduced == null || fixed == null;
        }

        boolean contains(String version) {
            int[] v = parse(version);
            if (v == null) {
                return false;
            }
            return compare(v, introduced) >= 0 && compare(v, fixed) < 0;
        }

        static int[] parse(String version) {
            if (version == null || version.isBlank()) {
                return null;
            }
            String[] parts = version.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            int[] out = new int[3];
            try {
                for (int i = 0; i < 3; i++) {
                    out[i] = Integer.parseInt(parts[i].trim());
                    if (out[i] < 0) {
                        return null;
                    }
                }
            } catch (NumberFormatException ex) {
                return null;
            }
            return out;
        }

        private static int compare(int[] a, int[] b) {
            for (int i = 0; i < 3; i++) {
                int c = Integer.compare(a[i], b[i]);
                if (c != 0) {
                    return c;
                }
            }
            return 0;
        }
    }
}
