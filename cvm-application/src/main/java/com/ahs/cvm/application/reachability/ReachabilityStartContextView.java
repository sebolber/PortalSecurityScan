package com.ahs.cvm.application.reachability;

import java.util.UUID;

/**
 * Iteration 97 (CVM-339): Vorbelegung fuer den Reachability-Start-
 * Dialog. Liefert Repo-URL (aus {@code Product.repoUrl}) und
 * Commit-SHA (aus {@code ProductVersion.gitCommit}) fuer das
 * Finding, sodass das UI die Pflichtfelder nicht manuell erfragen
 * muss.
 *
 * <p>Felder koennen {@code null} sein, wenn am Produkt keine
 * Repo-URL oder an der Version kein Commit hinterlegt ist. Die
 * {@code rationale} hilft dem UI, dem Nutzer einen sinnvollen
 * Hinweis zu geben (z.B. &quot;Produkt hat keine Repo-URL - bitte
 * manuell eintragen&quot;).
 */
public record ReachabilityStartContextView(
        UUID findingId,
        String repoUrl,
        String commitSha,
        String rationale) {}
