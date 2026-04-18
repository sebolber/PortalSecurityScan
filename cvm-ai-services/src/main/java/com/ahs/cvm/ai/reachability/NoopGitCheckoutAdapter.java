package com.ahs.cvm.ai.reachability;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default-Adapter fuer Tests/Sandbox. Legt ein leeres Verzeichnis im
 * tmpdir an, simuliert damit den Git-Checkout. Echter Clone ueber
 * JGit wird in einer Folge-Iteration ergaenzt, sobald die Vault-
 * Anbindung fuer SSH-Keys steht.
 *
 * <p>{@code @ConditionalOnMissingBean(name = ...)} prueft auf den
 * benamten JGit-Adapter-Bean (z.&nbsp;B. {@code jgitGitCheckoutAdapter}),
 * nicht auf die eigene Klasse - sonst wuerde sich die Bean selbst
 * ausschliessen und es gaebe gar keinen {@link GitCheckoutPort}.
 */
@Component
@ConditionalOnMissingBean(name = "jgitGitCheckoutAdapter")
public class NoopGitCheckoutAdapter implements GitCheckoutPort {

    private static final Logger log = LoggerFactory.getLogger(NoopGitCheckoutAdapter.class);

    @Override
    public Path checkout(String repoUrl, String branch, String commitSha) {
        try {
            Path dir = Files.createTempDirectory("cvm-reach-");
            log.debug("NoopGitCheckout: simuliert {}@{} unter {}",
                    repoUrl, commitSha, dir);
            return dir;
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "tmpdir nicht anlegbar: " + ex.getMessage(), ex);
        }
    }
}
