package com.ahs.cvm.domain.purl;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Iteration 58 (CVM-108): Kanonisiert PURLs (package-url) so, dass
 * kleinere kosmetische Unterschiede (Gross-/Kleinschreibung, Reihenfolge
 * der Qualifier, leere Qualifier) zwischen SBOM-Toolchains nicht dazu
 * fuehren, dass ein CVE-Treffer verpasst wird.
 *
 * <p>Regeln (bewusst minimal, stabil mit der PURL-Spec vertraeglich):
 * <ul>
 *   <li>{@code type}, {@code namespace}, {@code name} werden lowercase
 *       gesetzt (Ausnahmen wie {@code generic} behalten wir so).</li>
 *   <li>Qualifier werden alphabetisch nach Key sortiert; leere Werte
 *       werden entfernt.</li>
 *   <li>Fuehrende und nachfolgende Slashes werden normalisiert.</li>
 *   <li>{@code #subpath} bleibt unveraendert (Case-sensitive).</li>
 * </ul>
 *
 * <p>Die Klasse parst *nicht* jeden Edge-Case der Spezifikation. Bei
 * Eingaben ohne {@code pkg:}-Praefix oder mit offensichtlich kaputtem
 * Format wird der Originalstring zurueckgegeben; ein Fehler waere hier
 * fachlich unangemessen, weil PURLs aus beliebigen SBOM-Quellen kommen.
 */
public final class PurlCanonicalizer {

    private PurlCanonicalizer() {}

    public static String canonicalize(String purl) {
        if (purl == null) {
            return null;
        }
        String trimmed = purl.trim();
        if (!trimmed.startsWith("pkg:")) {
            return trimmed;
        }
        // Trenne fragment (#subpath) ab.
        String rest = trimmed.substring("pkg:".length());
        String subpath = null;
        int hashIdx = rest.indexOf('#');
        if (hashIdx >= 0) {
            subpath = rest.substring(hashIdx + 1);
            rest = rest.substring(0, hashIdx);
        }
        // Trenne qualifier (?...)
        String qualifierPart = null;
        int qIdx = rest.indexOf('?');
        if (qIdx >= 0) {
            qualifierPart = rest.substring(qIdx + 1);
            rest = rest.substring(0, qIdx);
        }
        // Teile in type/path (versionInclusive)
        int slashIdx = rest.indexOf('/');
        if (slashIdx < 0) {
            return trimmed;
        }
        String type = rest.substring(0, slashIdx).toLowerCase(Locale.ROOT);
        String pathWithVersion = rest.substring(slashIdx + 1);

        // Trenne version ab (am letzten '@').
        String path = pathWithVersion;
        String version = null;
        int atIdx = pathWithVersion.lastIndexOf('@');
        if (atIdx >= 0) {
            path = pathWithVersion.substring(0, atIdx);
            version = pathWithVersion.substring(atIdx + 1);
        }
        // namespace und name.
        String namespace = null;
        String name;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            namespace = path.substring(0, lastSlash).toLowerCase(Locale.ROOT);
            name = path.substring(lastSlash + 1).toLowerCase(Locale.ROOT);
        } else {
            name = path.toLowerCase(Locale.ROOT);
        }

        StringBuilder out = new StringBuilder("pkg:").append(type).append('/');
        if (namespace != null && !namespace.isEmpty()) {
            out.append(trimSlashes(namespace)).append('/');
        }
        out.append(name);
        if (version != null && !version.isEmpty()) {
            out.append('@').append(version);
        }
        String sortedQualifiers = sortAndFilterQualifiers(qualifierPart);
        if (sortedQualifiers != null && !sortedQualifiers.isEmpty()) {
            out.append('?').append(sortedQualifiers);
        }
        if (subpath != null && !subpath.isEmpty()) {
            out.append('#').append(subpath);
        }
        return out.toString();
    }

    private static String trimSlashes(String value) {
        String v = value;
        while (v.startsWith("/")) {
            v = v.substring(1);
        }
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    private static String sortAndFilterQualifiers(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        Map<String, String> sorted = new TreeMap<>();
        for (String part : raw.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            int eq = part.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = part.substring(0, eq).toLowerCase(Locale.ROOT);
            String value = part.substring(eq + 1);
            if (!key.isEmpty() && !value.isEmpty()) {
                sorted.put(key, value);
            }
        }
        if (sorted.isEmpty()) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (out.length() > 0) {
                out.append('&');
            }
            out.append(e.getKey()).append('=').append(e.getValue());
        }
        return out.toString();
    }

    public static boolean sameAfterCanonicalization(String a, String b) {
        return Objects.equals(canonicalize(a), canonicalize(b));
    }
}
