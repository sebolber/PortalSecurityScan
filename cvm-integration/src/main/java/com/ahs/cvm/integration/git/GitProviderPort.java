package com.ahs.cvm.integration.git;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Abstraktion ueber Upstream-Git-Provider (GitHub, GitLab, ...).
 * Liefert Release-Notes zwischen zwei Tags und die Commit-Liste im
 * gleichen Range (Iteration 16, CVM-41).
 *
 * <p>Der Port lebt im {@code cvm-integration}-Modul, weil er
 * HTTP-Calls an externe Anbieter macht. Implementierungen sind:
 *
 * <ul>
 *   <li>{@code GitHubApiProvider} - echter HTTP-Adapter.</li>
 *   <li>{@code FakeGitProvider} - liefert konfigurierte Testdaten,
 *       CI-Default.</li>
 * </ul>
 */
public interface GitProviderPort {

    Optional<ReleaseNotes> releaseNotes(String repoUrl, String tag);

    List<CommitSummary> compare(String repoUrl, String fromTag, String toTag);

    /**
     * Postet einen Kommentar an ein bestehendes Merge-/Pull-Request
     * (Iteration 22, CVM-53).
     *
     * <p>Wird z.&nbsp;B. vom CI/CD-Gate genutzt, um das Gate-Ergebnis
     * zurueck an den MR zu haengen. Provider-Fehler (Netzwerk,
     * HTTP 4xx/5xx, fehlende Berechtigung) fangen die Adapter ab und
     * liefern {@code false}; der Aufrufer entscheidet selbst, ob das
     * ein harter Fehler ist.
     *
     * @param repoUrl vollstaendige URL des Upstream-Repos.
     * @param mergeRequestId provider-spezifischer MR-/PR-Identifier
     *     (GitHub: PR-Nummer als String, GitLab: IID).
     * @param body Markdown-Body des Kommentars.
     * @return {@code true}, wenn der Provider den Post quittiert hat.
     */
    default boolean postMergeRequestComment(String repoUrl,
            String mergeRequestId, String body) {
        return false;
    }

    /**
     * @param repoUrl vollstaendige URL des Upstream-Repos
     *     (z.&nbsp;B. {@code https://github.com/foo/bar}).
     * @param tag Tag-Name (z.&nbsp;B. {@code v1.14.3}).
     * @param body Rohtext der Release-Notes (Markdown).
     * @param publishedAt Publikations-Zeitpunkt.
     * @param url Permalink auf den Release-Eintrag.
     */
    record ReleaseNotes(
            String repoUrl,
            String tag,
            String body,
            Instant publishedAt,
            String url) {}

    /**
     * Ein Commit in einer Compare-Range.
     *
     * @param sha Commit-SHA.
     * @param message Commit-Message (max. 2000 Zeichen im Service gekappt).
     * @param url Permalink.
     * @param author Author-Login / E-Mail.
     * @param date Commit-Zeitpunkt.
     * @param filesTouched Liste der betroffenen Dateinamen (kann
     *     leer sein, wenn Provider sie nicht mitliefert).
     */
    record CommitSummary(
            String sha,
            String message,
            String url,
            String author,
            Instant date,
            List<String> filesTouched) {

        public CommitSummary {
            filesTouched = filesTouched == null
                    ? List.of() : List.copyOf(filesTouched);
        }
    }
}
