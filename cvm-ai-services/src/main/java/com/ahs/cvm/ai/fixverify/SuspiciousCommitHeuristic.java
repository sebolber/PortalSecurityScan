package com.ahs.cvm.ai.fixverify;

import com.ahs.cvm.integration.git.GitProviderPort.CommitSummary;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Regel-basierte Vorfilterung von Commits (Iteration 16, CVM-41).
 * Liefert pro Commit ein {@link Verdict} mit Trefferbegruendung und
 * einem booleschen "verdaechtig"-Flag. Der LLM-Call bekommt nur die
 * verdaechtigen Commits als Volltext; der Rest fliesst als
 * Messages-Only in den Prompt.
 */
@Component
public class SuspiciousCommitHeuristic {

    private static final Pattern CVE_ID = Pattern.compile("CVE-\\d{4}-\\d{4,7}");
    private static final Pattern GHSA_ID =
            Pattern.compile("GHSA(-[a-z0-9]{4}){3}", Pattern.CASE_INSENSITIVE);
    private static final List<String> KEYWORDS = List.of(
            "security", "vulnerability", "xxe", "deserialization",
            "deserialisation", "rce", "xss", "injection",
            "path traversal", "ssrf", "denial of service", "dos",
            "prototype pollution", "redos", "sanitize", "escape",
            "cve-");

    public Verdict classify(CommitSummary commit, String vulnerableSymbol,
            String cveKey) {
        if (commit == null || commit.message() == null) {
            return Verdict.notSuspicious(commit);
        }
        String msg = commit.message();
        String lower = msg.toLowerCase(Locale.ROOT);

        if (cveKey != null && !cveKey.isBlank() && msg.contains(cveKey)) {
            return new Verdict(commit, true, "CVE-ID " + cveKey + " in Commit-Message.");
        }
        if (CVE_ID.matcher(msg).find()) {
            return new Verdict(commit, true, "CVE-ID in Commit-Message.");
        }
        if (GHSA_ID.matcher(msg).find()) {
            return new Verdict(commit, true, "GHSA-ID in Commit-Message.");
        }
        for (String kw : KEYWORDS) {
            if (lower.contains(kw)) {
                return new Verdict(commit, true, "Security-Keyword: " + kw);
            }
        }
        if (vulnerableSymbol != null && !vulnerableSymbol.isBlank()) {
            String symbol = vulnerableSymbol.toLowerCase(Locale.ROOT);
            String fileHint = fileBasisName(symbol);
            if (lower.contains(fileHint)) {
                return new Verdict(commit, true,
                        "Vulnerable Symbol im Commit-Text erwaehnt.");
            }
            for (String file : commit.filesTouched()) {
                if (file != null && file.toLowerCase(Locale.ROOT).contains(fileHint)) {
                    return new Verdict(commit, true,
                            "Vulnerable Symbol in geaenderter Datei " + file);
                }
            }
        }
        return Verdict.notSuspicious(commit);
    }

    static String fileBasisName(String vulnerableSymbol) {
        // "com.example.Foo.bar(Type)" -> "foo"
        int paren = vulnerableSymbol.indexOf('(');
        String head = paren >= 0 ? vulnerableSymbol.substring(0, paren)
                : vulnerableSymbol;
        int method = head.lastIndexOf('.');
        if (method < 0) {
            return head;
        }
        String klasse = head.substring(0, method);
        int pkg = klasse.lastIndexOf('.');
        return (pkg < 0 ? klasse : klasse.substring(pkg + 1)).toLowerCase(Locale.ROOT);
    }

    public record Verdict(CommitSummary commit, boolean suspicious, String reason) {

        public static Verdict notSuspicious(CommitSummary c) {
            return new Verdict(c, false, "keine Heuristik getroffen");
        }
    }
}
