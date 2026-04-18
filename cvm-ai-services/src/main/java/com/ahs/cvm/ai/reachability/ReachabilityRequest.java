package com.ahs.cvm.ai.reachability;

import java.util.UUID;

/**
 * Eingabe fuer den {@link ReachabilityAgent}.
 *
 * @param findingId Pflicht.
 * @param repoUrl Pflicht: Git-URL (https oder ssh).
 * @param branch optional: Branch (Default {@code main}).
 * @param commitSha optional: exakter Commit (Default
 *     {@code ProductVersion.gitCommit}, wenn vorhanden).
 * @param vulnerableSymbol z.&nbsp;B. {@code com.x.Y.method(Type)}.
 * @param language {@code java|typescript|python}.
 * @param instruction Optionaler Bewerter-Hinweis.
 * @param triggeredBy Login fuer Audit.
 */
public record ReachabilityRequest(
        UUID findingId,
        String repoUrl,
        String branch,
        String commitSha,
        String vulnerableSymbol,
        String language,
        String instruction,
        String triggeredBy) {

    public ReachabilityRequest {
        if (findingId == null) {
            throw new IllegalArgumentException("findingId darf nicht null sein.");
        }
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new IllegalArgumentException("repoUrl darf nicht leer sein.");
        }
        if (vulnerableSymbol == null || vulnerableSymbol.isBlank()) {
            throw new IllegalArgumentException("vulnerableSymbol darf nicht leer sein.");
        }
        if (triggeredBy == null || triggeredBy.isBlank()) {
            throw new IllegalArgumentException("triggeredBy darf nicht leer sein.");
        }
        if (branch == null || branch.isBlank()) {
            branch = "main";
        }
        if (language == null || language.isBlank()) {
            language = "java";
        }
        if (instruction == null) {
            instruction = "";
        }
    }
}
