package com.ahs.cvm.ai.reachability;

import java.nio.file.Path;

/**
 * Port fuer Git-Checkouts (Iteration 15, CVM-40). Klont oder fetched
 * den angegebenen Commit eines Repos in einen lokalen Cache und
 * liefert den Arbeitsverzeichnis-Pfad zurueck.
 *
 * <p>Default-Implementation ist {@link NoopGitCheckoutAdapter} (kein
 * tatsaechlicher Clone, fuer CI-Tests). Eine echte JGit-Implementation
 * folgt sobald die Vault-/SSH-Anbindung steht.
 */
public interface GitCheckoutPort {

    /**
     * Stellt sicher, dass {@code commitSha} aus {@code repoUrl} im
     * Cache liegt, und liefert den Arbeitsverzeichnis-Pfad.
     *
     * @param repoUrl https- oder SSH-URL.
     * @param branch optionaler Branch-Hint (Pflicht fuer Initial-Clone).
     * @param commitSha exakter Commit (40 Zeichen Hex).
     * @return Pfad zum Working-Directory.
     */
    Path checkout(String repoUrl, String branch, String commitSha);
}
