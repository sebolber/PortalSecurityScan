package com.ahs.cvm.application.reachability;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * Leitet aus einer PURL einen sinnvollen Default fuer das
 * {@code vulnerableSymbol}-Feld der Reachability-Analyse ab.
 *
 * <p>Der Deriver liefert nie die exakte verwundbare Methode - nur
 * einen Package-/Namespace-Hinweis, den Claude-Code als Einstieg fuer
 * die Suche nutzen kann. Der Analyst kann den Vorschlag im Dialog
 * weiter praezisieren (z.B. auf {@code org.apache.commons.text.StringSubstitutor#replace}).
 *
 * <p>Die Klasse ist bewusst stateless und testbar ohne Spring -
 * Verhalten ist pure Funktion von PURL-String zu
 * {@link Suggestion}.
 */
public final class PurlSymbolDeriver {

    private PurlSymbolDeriver() {}

    /**
     * @return Suggestion mit abgeleitetem Symbol und Sprache, oder
     *         {@link Optional#empty()} wenn die PURL unbekannt /
     *         nicht parsebar ist. In dem Fall muss der Analyst das
     *         Symbol manuell eingeben.
     */
    public static Optional<Suggestion> derive(String purl) {
        if (purl == null) {
            return Optional.empty();
        }
        String trimmed = purl.trim();
        if (!trimmed.startsWith("pkg:")) {
            return Optional.empty();
        }
        // Strippen wir Version und Qualifier/Subpath ab: "pkg:type/ns/name@1.0?x=y#sub"
        String ohneVersion = trimmed.split("[?#]", 2)[0];
        int at = ohneVersion.indexOf('@');
        if (at >= 0) {
            ohneVersion = ohneVersion.substring(0, at);
        }
        String rest = ohneVersion.substring("pkg:".length());
        int firstSlash = rest.indexOf('/');
        if (firstSlash <= 0 || firstSlash == rest.length() - 1) {
            return Optional.empty();
        }
        String type = rest.substring(0, firstSlash).toLowerCase(Locale.ROOT);
        String coords = rest.substring(firstSlash + 1);

        return switch (type) {
            case "maven" -> maven(coords);
            case "npm" -> npm(coords);
            case "pypi" -> pypi(coords);
            case "golang" -> golang(coords);
            case "cargo" -> cargo(coords);
            case "nuget" -> nuget(coords);
            case "gem" -> gem(coords);
            case "composer" -> composer(coords);
            default -> Optional.empty();
        };
    }

    /**
     * Maven-Koordinaten sind {@code groupId/artifactId}. Die Java-
     * Package-Konvention ist in ~80% der Faelle
     * {@code groupId + '.' + artifactId (mit '-' → '.')}. Bei
     * Abweichungen (z.B. Jackson) liefert die groupId allein immer
     * einen brauchbaren Startpunkt.
     */
    private static Optional<Suggestion> maven(String coords) {
        String[] parts = coords.split("/", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return Optional.empty();
        }
        String groupId = decode(parts[0]);
        String artifactId = decode(parts[1]);
        String abgeleitetesPaket = groupId + "."
                + artifactId.replace('-', '.').replace('_', '.');
        // Wenn das abgeleitete Paket mit dem groupId anfaengt + ein
        // zusaetzliches Suffix hat, ist es wahrscheinlich korrekt
        // (z.B. org.apache.commons + commons-text → org.apache.commons.commons.text).
        // Das ist hollprig; gleichzeitig ist groupId allein zu generisch.
        // Kompromiss: Wenn artifactId eine Praefix-Ueberlappung mit dem
        // letzten groupId-Segment hat (commons-text bei groupId ...commons),
        // streichen wir das Duplikat.
        String letztesSegment = letztesSegment(groupId);
        String ersteArtifactKomponente = artifactId.split("[-_]", 2)[0];
        if (letztesSegment.equals(ersteArtifactKomponente)) {
            String rest = artifactId.length() > ersteArtifactKomponente.length()
                    ? artifactId.substring(ersteArtifactKomponente.length() + 1)
                            .replace('-', '.').replace('_', '.')
                    : "";
            abgeleitetesPaket = rest.isBlank() ? groupId : groupId + "." + rest;
        }
        return Optional.of(new Suggestion(abgeleitetesPaket, "java",
                "Paket-Prefix aus Maven-Koordinaten; bei Bedarf auf Klasse#methode verfeinern."));
    }

    private static Optional<Suggestion> npm(String coords) {
        String c = decode(coords);
        // @scope/name ist erlaubt; wir lassen ihn stehen.
        if (c.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Suggestion(c, "javascript",
                "npm-Paketname; fuer TypeScript-Projekte Sprache auf 'typescript' stellen."));
    }

    private static Optional<Suggestion> pypi(String coords) {
        String c = decode(coords).toLowerCase(Locale.ROOT).replace('-', '_');
        if (c.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Suggestion(c, "python",
                "Python-Modulname (aus PyPI-Paketname abgeleitet)."));
    }

    private static Optional<Suggestion> golang(String coords) {
        String c = decode(coords);
        if (c.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Suggestion(c, "go",
                "Go-Import-Pfad; fuer exakte Analyse Symbol wie 'Pfad.Func' angeben."));
    }

    private static Optional<Suggestion> cargo(String coords) {
        String c = decode(coords);
        if (c.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Suggestion(c, "rust",
                "Crate-Name; fuer exakte Analyse 'crate::modul::Typ::methode' angeben."));
    }

    private static Optional<Suggestion> nuget(String coords) {
        String c = decode(coords);
        if (c.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Suggestion(c, "csharp",
                "NuGet-Paketname."));
    }

    private static Optional<Suggestion> gem(String coords) {
        String c = decode(coords);
        if (c.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Suggestion(c, "ruby",
                "Ruby-Gem-Name."));
    }

    private static Optional<Suggestion> composer(String coords) {
        String c = decode(coords);
        if (c.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Suggestion(c, "php",
                "Composer-Paketname (vendor/paket)."));
    }

    private static String letztesSegment(String dotted) {
        int idx = dotted.lastIndexOf('.');
        return idx < 0 ? dotted : dotted.substring(idx + 1);
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    /**
     * @param symbol Abgeleitetes Symbol (Package-Prefix oder Paketname).
     * @param language Sprach-Hint fuer das Frontend-Dropdown (z.B. {@code java}).
     * @param rationale Kurze menschliche Erklaerung, was abgeleitet wurde.
     */
    public record Suggestion(String symbol, String language, String rationale) {}
}
