package com.ahs.cvm.ai.reachability;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Echte Git-Checkout-Implementierung fuer den Reachability-Agent
 * (Iteration 71, CVM-308). Clont pro eindeutigem
 * {@code (repoUrl, commitSha)}-Paar genau einmal und legt das
 * Arbeitsverzeichnis unterhalb des Cache-Roots ab.
 *
 * <p>Der Bean-Name {@code jgitGitCheckoutAdapter} deckt die in
 * {@link NoopGitCheckoutAdapter} dokumentierte
 * {@code @ConditionalOnMissingBean}-Annotation ab: sobald dieser
 * Adapter im Context ist, wird der Noop-Fallback nicht erzeugt.
 *
 * <p>HTTPS-Authentifizierung erfolgt ueber den optionalen
 * Parameter-Store-Wert {@code cvm.ai.reachability.git.https-token}
 * (AES-GCM verschluesselt). SSH (ssh-agent / Vault-SSH-Key) ist
 * noch nicht implementiert - siehe offene Punkte.
 */
@Component("jgitGitCheckoutAdapter")
public class JGitGitCheckoutAdapter implements GitCheckoutPort {

    private static final Logger log = LoggerFactory.getLogger(
            JGitGitCheckoutAdapter.class);

    private static final String PARAM_CACHE_DIR = "cvm.ai.reachability.git.cache-dir";
    private static final String PARAM_CACHE_TTL = "cvm.ai.reachability.git.cache-ttl-hours";
    private static final String PARAM_HTTPS_TOKEN = "cvm.ai.reachability.git.https-token";

    private final Path defaultCacheDir;
    private final int defaultCacheTtlHours;
    private final Optional<SystemParameterResolver> parameterResolver;

    public JGitGitCheckoutAdapter(
            @Value("${cvm.ai.reachability.git.cache-dir:}") String cacheDir,
            @Value("${cvm.ai.reachability.git.cache-ttl-hours:72}") int cacheTtlHours,
            Optional<SystemParameterResolver> parameterResolver) {
        this.defaultCacheDir = resolveCacheDir(cacheDir);
        this.defaultCacheTtlHours = Math.max(1, cacheTtlHours);
        this.parameterResolver = parameterResolver == null
                ? Optional.empty()
                : parameterResolver;
    }

    @Override
    public Path checkout(String repoUrl, String branch, String commitSha) {
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new IllegalArgumentException("repoUrl darf nicht leer sein.");
        }
        if (commitSha == null || commitSha.isBlank()) {
            throw new IllegalArgumentException("commitSha darf nicht leer sein.");
        }
        Path cacheRoot = effectiveCacheRoot();
        Path target = cacheRoot.resolve(cacheKey(repoUrl, commitSha));
        if (Files.exists(target.resolve(".git"))) {
            log.debug("JGitCheckout Cache-Treffer: {}", target);
            return target;
        }
        try {
            Files.createDirectories(cacheRoot);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cache-Verzeichnis nicht anlegbar: " + cacheRoot, e);
        }

        CloneCommand clone = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(target.toFile())
                .setNoCheckout(true);
        if (branch != null && !branch.isBlank()) {
            clone.setBranch(branch);
        }
        CredentialsProvider credentials = credentialsProvider(repoUrl);
        if (credentials != null) {
            clone.setCredentialsProvider(credentials);
        }

        try (Git git = clone.call()) {
            git.checkout().setName(commitSha).call();
            log.info("JGitCheckout: {}@{} -> {}", repoUrl, commitSha, target);
            return target;
        } catch (Exception ex) {
            // Nach Fehler den halb-geclonten Ordner aufraeumen, damit der
            // naechste Versuch sauber neu starten kann.
            pfadLoeschen(target);
            throw new IllegalStateException(
                    "JGit-Clone fehlgeschlagen fuer " + repoUrl + "@" + commitSha
                            + ": " + ex.getMessage(),
                    ex);
        }
    }

    /**
     * Cleanup-Job: loescht Cache-Eintraege, deren
     * letzte-Aenderung-Zeit aelter als die TTL ist.
     * Laueft taeglich um 03:00 Uhr im JVM-Zeit-Zonen-Kontext.
     */
    @Scheduled(cron = "${cvm.ai.reachability.git.cache-cleanup-cron:0 0 3 * * *}")
    public void cleanupCache() {
        Path cacheRoot = effectiveCacheRoot();
        if (!Files.isDirectory(cacheRoot)) {
            return;
        }
        int ttlHours = effectiveTtlHours();
        Instant cutoff = Instant.now().minus(Duration.ofHours(ttlHours));
        int entfernt = 0;
        try (Stream<Path> stream = Files.list(cacheRoot)) {
            for (Path entry : stream.toList()) {
                Instant lastModified = Files.getLastModifiedTime(entry).toInstant();
                if (lastModified.isBefore(cutoff)) {
                    pfadLoeschen(entry);
                    entfernt++;
                }
            }
            if (entfernt > 0) {
                log.info("JGitCheckout Cleanup: {} Eintraege entfernt (ttl={}h)",
                        entfernt, ttlHours);
            }
        } catch (IOException ex) {
            log.warn("JGitCheckout Cleanup fehlgeschlagen: {}", ex.getMessage());
        }
    }

    private Path effectiveCacheRoot() {
        String override = parameterResolver
                .flatMap(r -> r.resolve(PARAM_CACHE_DIR))
                .filter(v -> !v.isBlank())
                .orElse(null);
        return override != null ? Path.of(override) : defaultCacheDir;
    }

    private int effectiveTtlHours() {
        return parameterResolver
                .map(r -> r.resolveInt(PARAM_CACHE_TTL, defaultCacheTtlHours))
                .orElse(defaultCacheTtlHours);
    }

    private CredentialsProvider credentialsProvider(String repoUrl) {
        if (repoUrl == null || !repoUrl.startsWith("http")) {
            return null;
        }
        String token = parameterResolver
                .flatMap(r -> r.resolve(PARAM_HTTPS_TOKEN))
                .filter(v -> !v.isBlank())
                .orElse(null);
        if (token == null) {
            return null;
        }
        // GitHub/GitLab-Konvention: Token als Username, beliebiges Passwort-
        // Feld. Fuer Basic-Auth waere stattdessen "user:token" noetig.
        return new UsernamePasswordCredentialsProvider(token, "");
    }

    private static Path resolveCacheDir(String configured) {
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        return Path.of(System.getProperty("java.io.tmpdir"))
                .resolve("cvm-reach-cache");
    }

    private static String cacheKey(String repoUrl, String commitSha) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(repoUrl.getBytes());
            md.update((byte) 0);
            md.update(commitSha.getBytes());
            return HexFormat.of().formatHex(md.digest()).substring(0, 16)
                    + "-" + commitSha.substring(0, Math.min(12, commitSha.length()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nicht verfuegbar", e);
        }
    }

    private static void pfadLoeschen(Path target) {
        if (target == null || !Files.exists(target)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(target)) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ex) {
            log.warn("Cleanup von {} fehlgeschlagen: {}", target, ex.getMessage());
        }
    }
}
