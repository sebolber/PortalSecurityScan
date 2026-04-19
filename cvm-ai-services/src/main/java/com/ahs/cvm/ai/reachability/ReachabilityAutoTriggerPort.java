package com.ahs.cvm.ai.reachability;

import java.util.UUID;

/**
 * Port fuer den eigentlichen Reachability-Aufruf aus dem
 * Auto-Trigger (Iteration 70, CVM-307).
 *
 * <p>Die Default-Implementierung
 * ({@link NoopReachabilityAutoTriggerAdapter}) loggt nur; die
 * tatsaechliche Integration mit dem {@link ReachabilityAgent}
 * (inklusive {@link GitCheckoutPort}) folgt in Iteration 71
 * zusammen mit dem JGit-Adapter. Damit bleibt die Schwellwert-
 * und Rate-Limit-Logik in dieser Iteration eigenstaendig
 * testbar, ohne Subprocess- oder Git-Abhaengigkeiten.
 */
@FunctionalInterface
public interface ReachabilityAutoTriggerPort {

    /**
     * Startet einen Reachability-Lauf fuer das angegebene Finding.
     * Die Implementierung verantwortet u.a. das Auflosen von
     * {@code repoUrl}, {@code branch}, {@code commitSha} und
     * {@code vulnerableSymbol} aus Finding + ProduktVersion.
     *
     * @param findingId Finding, fuer das der Lauf angestossen wird.
     * @param triggeredBy Login fuer das Audit (typisch
     *                    {@code system:auto-trigger}).
     */
    void trigger(UUID findingId, String triggeredBy);
}
