package com.ahs.cvm.application.cve;

import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.cve.CveRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importiert CVE-Daten aus einer hochgeladenen JSON-Datei. Akzeptiert
 * das NVD-2.0-Schema (Objekt mit {@code vulnerabilities[]}, jeder Eintrag
 * unter {@code cve}) sowie eine einfache flache Liste von CVE-Objekten.
 *
 * <p>Motivation (Iteration 61): Fuer air-gapped Installationen und fuer
 * Dev-/Test-Setups soll ein Admin CVE-Daten ohne Outbound-Traffic in den
 * Store bringen koennen.
 *
 * <p>Idempotenz: Ein CVE mit gleicher {@code cve_id} wird geupdated, nicht
 * dupliziert. Felder werden nur ueberschrieben, wenn die Quelle einen
 * Wert liefert; bereits gesetzte Felder bleiben erhalten, wenn die Datei
 * null/leer liefert.
 */
@Service
public class CveFeedImportService {

    private static final Logger log = LoggerFactory.getLogger(CveFeedImportService.class);

    private final CveRepository cveRepository;
    private final ObjectMapper objectMapper;

    public CveFeedImportService(CveRepository cveRepository, ObjectMapper objectMapper) {
        this.cveRepository = cveRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportReport importFrom(InputStream in, String sourceLabel) throws IOException {
        JsonNode root = objectMapper.readTree(in);
        List<JsonNode> cves = new ArrayList<>();
        if (root.isObject() && root.has("vulnerabilities") && root.get("vulnerabilities").isArray()) {
            for (JsonNode v : root.get("vulnerabilities")) {
                JsonNode cveNode = v.path("cve");
                if (cveNode.isObject() && cveNode.has("id")) {
                    cves.add(cveNode);
                }
            }
        } else if (root.isArray()) {
            for (JsonNode cveNode : root) {
                if (cveNode.isObject() && (cveNode.has("id") || cveNode.has("cveId"))) {
                    cves.add(cveNode);
                }
            }
        } else if (root.isObject() && (root.has("id") || root.has("cveId"))) {
            cves.add(root);
        } else {
            throw new IllegalArgumentException(
                    "Unbekanntes Format. Erwartet: NVD-2.0 ({\"vulnerabilities\":[...]}) "
                            + "oder Array/Objekt mit \"id\"/\"cveId\".");
        }

        String source = (sourceLabel == null || sourceLabel.isBlank()) ? "UPLOAD" : sourceLabel.trim().toUpperCase();
        int angelegt = 0;
        int aktualisiert = 0;
        List<String> gescheitert = new ArrayList<>();

        for (JsonNode cveNode : cves) {
            try {
                String cveId = cveNode.has("id") ? cveNode.get("id").asText()
                        : cveNode.get("cveId").asText();
                if (cveId == null || cveId.isBlank()) {
                    gescheitert.add("(fehlende cveId)");
                    continue;
                }
                boolean neu = cveRepository.findByCveId(cveId).isEmpty();
                Cve cve = cveRepository.findByCveId(cveId).orElseGet(
                        () -> Cve.builder().cveId(cveId).source(source).build());
                uebernehme(cve, cveNode, source);
                cve.setLastFetchedAt(Instant.now());
                cveRepository.save(cve);
                if (neu) {
                    angelegt++;
                } else {
                    aktualisiert++;
                }
            } catch (RuntimeException e) {
                log.warn("CVE-Import fehlgeschlagen: {}", e.getMessage());
                gescheitert.add(e.getMessage() == null ? "unbekannter Fehler" : e.getMessage());
            }
        }

        log.info("CVE-Import abgeschlossen: {} neu, {} aktualisiert, {} fehlgeschlagen (Quelle: {})",
                angelegt, aktualisiert, gescheitert.size(), source);
        return new ImportReport(cves.size(), angelegt, aktualisiert, gescheitert);
    }

    private void uebernehme(Cve cve, JsonNode cveNode, String source) {
        String summary = leseBeschreibung(cveNode);
        if (summary != null && !summary.isBlank()) {
            cve.setSummary(summary);
        } else if (cveNode.hasNonNull("summary")) {
            cve.setSummary(cveNode.get("summary").asText());
        }

        BigDecimal score = leseCvssScore(cveNode);
        if (score != null) {
            cve.setCvssBaseScore(score);
        }
        String vektor = leseCvssVector(cveNode);
        if (vektor != null) {
            cve.setCvssVector(vektor);
        }

        List<String> cwes = leseCwes(cveNode);
        if (!cwes.isEmpty()) {
            cve.setCwes(cwes);
        }

        Instant published = leseInstant(cveNode, "published", "publishedAt");
        if (published != null) {
            cve.setPublishedAt(published);
        }
        Instant modified = leseInstant(cveNode, "lastModified", "lastModifiedAt");
        if (modified != null) {
            cve.setLastModifiedAt(modified);
        }

        if (cve.getSource() == null || "STUB".equals(cve.getSource())) {
            cve.setSource(source);
        }
    }

    private String leseBeschreibung(JsonNode cve) {
        JsonNode descs = cve.path("descriptions");
        if (descs.isArray()) {
            String fallback = null;
            for (JsonNode d : descs) {
                String value = d.path("value").asText(null);
                if (value == null) continue;
                if ("en".equalsIgnoreCase(d.path("lang").asText())) {
                    return value;
                }
                if (fallback == null) fallback = value;
            }
            return fallback;
        }
        return null;
    }

    private BigDecimal leseCvssScore(JsonNode cve) {
        for (String key : new String[] {"cvssMetricV31", "cvssMetricV30", "cvssMetricV2"}) {
            JsonNode metrics = cve.path("metrics").path(key);
            if (metrics.isArray() && !metrics.isEmpty()) {
                JsonNode data = metrics.get(0).path("cvssData");
                if (data.has("baseScore")) {
                    return BigDecimal.valueOf(data.path("baseScore").asDouble());
                }
            }
        }
        if (cve.hasNonNull("cvssBaseScore")) {
            return BigDecimal.valueOf(cve.get("cvssBaseScore").asDouble());
        }
        return null;
    }

    private String leseCvssVector(JsonNode cve) {
        for (String key : new String[] {"cvssMetricV31", "cvssMetricV30", "cvssMetricV2"}) {
            JsonNode metrics = cve.path("metrics").path(key);
            if (metrics.isArray() && !metrics.isEmpty()) {
                JsonNode data = metrics.get(0).path("cvssData");
                if (data.has("vectorString")) {
                    return data.path("vectorString").asText();
                }
            }
        }
        if (cve.hasNonNull("cvssVector")) {
            return cve.get("cvssVector").asText();
        }
        return null;
    }

    private List<String> leseCwes(JsonNode cve) {
        List<String> out = new ArrayList<>();
        for (JsonNode w : cve.path("weaknesses")) {
            for (JsonNode d : w.path("description")) {
                String value = d.path("value").asText(null);
                if (value != null && value.startsWith("CWE-") && !out.contains(value)) {
                    out.add(value);
                }
            }
        }
        if (out.isEmpty() && cve.path("cwes").isArray()) {
            for (JsonNode c : cve.get("cwes")) {
                String v = c.asText(null);
                if (v != null && !out.contains(v)) out.add(v);
            }
        }
        return out;
    }

    private Instant leseInstant(JsonNode cve, String... fieldNames) {
        for (String field : fieldNames) {
            if (cve.hasNonNull(field)) {
                String raw = cve.get(field).asText();
                try {
                    return Instant.parse(raw);
                } catch (DateTimeParseException e) {
                    // NVD liefert manchmal Suffix ohne 'Z' - toleriere Offset-less
                    try {
                        return Instant.parse(raw + "Z");
                    } catch (DateTimeParseException e2) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public record ImportReport(int gefundeneEintraege, int angelegt, int aktualisiert,
                                List<String> fehler) {
        public ImportReport {
            fehler = fehler == null ? List.of() : List.copyOf(fehler);
        }

        public int fehlerCount() {
            return fehler.size();
        }

        /** Iterator-Hilfe fuer Logs/Tests. */
        public Iterator<String> fehlerIterator() {
            return fehler.iterator();
        }
    }
}
