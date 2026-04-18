package com.ahs.cvm.llm.injection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Regel-basierter Prompt-Injection-Detektor. Erkennt bekannte
 * Angriffsmuster in User-Prompts und RAG-Kontext:
 *
 * <ol>
 *   <li>"ignore previous" / "ignore above"</li>
 *   <li>Rollen-Marker wie {@code system:} im User-Eingabetext</li>
 *   <li>"you are now" / "act as" (Rollenwechsel)</li>
 *   <li>"disregard" / "forget" + instruction-Schluesselwoerter</li>
 *   <li>Zero-width-Unicode-Zeichen (U+200B, U+200C, U+200D, U+FEFF)</li>
 *   <li>Verdaechtig lange Base64-Bloecke (&gt;= 200 Zeichen)</li>
 *   <li>HTML-/Markdown-Header-Injection (z.B. {@code <system>})</li>
 *   <li>"reveal your prompt" / "what is your system prompt"</li>
 *   <li>"bypass" / "jailbreak" / "DAN mode"</li>
 *   <li>{@code {{ }} }-Doppel-Geschweifte-Klammern (Template-Injection)</li>
 *   <li>Null-Bytes / Steuerzeichen</li>
 * </ol>
 *
 * <p>Die Liste ist bewusst konservativ; False-Positives werden im
 * Warn-Modus nur getaggt, nicht geblockt. Der Block-Modus
 * ({@code cvm.llm.injection.mode=block}) dient Umgebungen, die keine
 * Toleranz fuer verdaechtige Eingaben haben (Prod, Mandant X).
 */
@Component
public class InjectionDetector {

    private static final Pattern BASE64_LANG = Pattern.compile("[A-Za-z0-9+/=]{200,}");
    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B\\u200C\\u200D\\uFEFF]");
    private static final Pattern CONTROL = Pattern.compile("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]");
    private static final Pattern DOUBLE_BRACES = Pattern.compile("\\{\\{.+?}}");
    private static final Pattern ROLE_MARKER =
            Pattern.compile("\\b(system|assistant)\\s*:",
                    Pattern.CASE_INSENSITIVE);

    private static final List<Heuristic> HEURISTIKEN = List.of(
            substring("IGNORE_PREVIOUS", "ignore previous"),
            substring("IGNORE_ABOVE", "ignore above"),
            substring("DISREGARD_INSTRUCTIONS", "disregard previous instructions"),
            substring("DISREGARD_ALL", "disregard all"),
            substring("FORGET_EVERYTHING", "forget everything"),
            substring("YOU_ARE_NOW", "you are now"),
            substring("ACT_AS", "act as "),
            substring("PRETEND_YOU_ARE", "pretend you are"),
            substring("REVEAL_PROMPT", "reveal your prompt"),
            substring("PRINT_SYSTEM_PROMPT", "print the system prompt"),
            substring("WHAT_IS_SYSTEM", "what is your system prompt"),
            substring("JAILBREAK", "jailbreak"),
            substring("DAN_MODE", "dan mode"),
            substring("BYPASS_RULES", "bypass"),
            substring("ADMIN_OVERRIDE", "admin override"),
            regex("ROLE_MARKER", ROLE_MARKER),
            regex("ZERO_WIDTH", ZERO_WIDTH),
            regex("CONTROL_CHAR", CONTROL),
            regex("BASE64_LARGE", BASE64_LANG),
            regex("DOUBLE_BRACES", DOUBLE_BRACES),
            substring("HTML_SYSTEM_TAG", "<system"),
            substring("HTML_INSTRUCTION_TAG", "<instructions"));

    /**
     * Wertet alle Heuristiken gegen den gegebenen Text aus und
     * liefert das Ergebnis mit getroffener Markerliste.
     */
    public InjectionVerdict check(String input) {
        if (input == null || input.isEmpty()) {
            return InjectionVerdict.clean();
        }
        String normalized = input.toLowerCase(Locale.ROOT);
        List<String> treffer = new ArrayList<>();
        for (Heuristic h : HEURISTIKEN) {
            if (h.matcher().test(input, normalized)) {
                treffer.add(h.marker());
            }
        }
        return treffer.isEmpty() ? InjectionVerdict.clean() : new InjectionVerdict(true, treffer);
    }

    /** Wertet mehrere Strings gemeinsam aus (z.B. User-Prompt + RAG-Kontext). */
    public InjectionVerdict checkAll(String... inputs) {
        List<String> gesammelt = new ArrayList<>();
        for (String input : inputs) {
            InjectionVerdict v = check(input);
            for (String m : v.marker()) {
                if (!gesammelt.contains(m)) {
                    gesammelt.add(m);
                }
            }
        }
        return gesammelt.isEmpty() ? InjectionVerdict.clean()
                : new InjectionVerdict(true, gesammelt);
    }

    private static Heuristic substring(String marker, String needle) {
        String lower = needle.toLowerCase(Locale.ROOT);
        return new Heuristic(marker, (raw, norm) -> norm.contains(lower));
    }

    private static Heuristic regex(String marker, Pattern pattern) {
        return new Heuristic(marker, (raw, norm) -> pattern.matcher(raw).find());
    }

    /** Ergebnis einer Injection-Analyse. */
    public record InjectionVerdict(boolean suspicious, List<String> marker) {

        public InjectionVerdict {
            marker = marker == null ? List.of() : List.copyOf(marker);
        }

        public static InjectionVerdict clean() {
            return new InjectionVerdict(false, List.of());
        }
    }

    private record Heuristic(String marker, Matcher matcher) {}

    @FunctionalInterface
    private interface Matcher {
        boolean test(String raw, String normalized);
    }
}
