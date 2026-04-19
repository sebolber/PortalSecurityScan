package com.ahs.cvm.ai.reachability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JGitGitCheckoutAdapterTest {

    @TempDir
    Path tempDir;

    private Path bareRepo;
    private String commitSha;
    private Path cacheRoot;
    private JGitGitCheckoutAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        bareRepo = tempDir.resolve("origin.git");
        Path working = tempDir.resolve("working");
        Files.createDirectories(working);

        try (Git git = Git.init().setDirectory(working.toFile()).call()) {
            // Lokale gpg.format=ssh-Einstellung aus User-Config lokal neutralisieren,
            // sonst weigert sich JGit zu committen.
            git.getRepository().getConfig().setString("gpg", null, "format", "openpgp");
            git.getRepository().getConfig().setBoolean("commit", null, "gpgsign", false);
            git.getRepository().getConfig().setBoolean("tag", null, "gpgsign", false);
            git.getRepository().getConfig().save();
            Files.writeString(working.resolve("README.md"), "# fake repo\n");
            git.add().addFilepattern("README.md").call();
            RevCommit commit = git.commit()
                    .setMessage("Initial commit")
                    .setAuthor("t.tester", "t.tester@ahs.test")
                    .setCommitter("t.tester", "t.tester@ahs.test")
                    .setSign(false)
                    .call();
            commitSha = commit.getName();
        }
        try (Git bare = Git.init().setBare(true).setDirectory(bareRepo.toFile()).call()) {
            // bare remote erstellt; wir pushen gleich aus working-dir.
        }
        try (Git origin = Git.open(tempDir.resolve("working").toFile())) {
            origin.remoteAdd().setName("origin")
                    .setUri(new org.eclipse.jgit.transport.URIish(
                            bareRepo.toUri().toString()))
                    .call();
            origin.push().setRemote("origin").setPushAll().call();
        }

        cacheRoot = tempDir.resolve("cache");
        adapter = new JGitGitCheckoutAdapter(
                cacheRoot.toString(), 72, Optional.empty());
    }

    @AfterEach
    void cleanup() throws Exception {
        if (Files.exists(tempDir)) {
            try (Stream<Path> stream = Files.walk(tempDir)) {
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @Test
    @DisplayName("JGit: erster Checkout klont das bare-Repo und laesst einen .git-Ordner zurueck")
    void ersterCheckoutKlont() {
        Path workdir = adapter.checkout(
                bareRepo.toUri().toString(), "master", commitSha);

        assertThat(workdir).exists();
        assertThat(workdir.resolve(".git")).exists();
        assertThat(workdir.resolve("README.md")).exists();
    }

    @Test
    @DisplayName("JGit: zweiter Checkout des gleichen Commits nutzt den Cache")
    void zweiterCheckoutNutztCache() {
        Path first = adapter.checkout(
                bareRepo.toUri().toString(), "master", commitSha);
        long firstMtime = first.toFile().lastModified();
        Path second = adapter.checkout(
                bareRepo.toUri().toString(), "master", commitSha);

        assertThat(second).isEqualTo(first);
        assertThat(second.toFile().lastModified()).isEqualTo(firstMtime);
    }

    @Test
    @DisplayName("JGit: leerer commitSha wirft IllegalArgument")
    void leerCommitShaWirft() {
        assertThatThrownBy(() -> adapter.checkout(
                bareRepo.toUri().toString(), "master", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("JGit: cleanupCache entfernt abgelaufene Cache-Eintraege")
    void cleanupEntferntAlteEintraege() throws Exception {
        Path workdir = adapter.checkout(
                bareRepo.toUri().toString(), "master", commitSha);
        // Uhr zuruecksetzen, damit der Cache-Eintrag als "alt" gilt.
        Files.setLastModifiedTime(workdir,
                java.nio.file.attribute.FileTime.fromMillis(0L));

        adapter.cleanupCache();

        assertThat(workdir).doesNotExist();
    }

    @Test
    @DisplayName("JGit: Parameter-Store-Override fuer cache-dir greift")
    void cacheDirOverrideViaResolver() {
        Path alternativerCache = tempDir.resolve("override-cache");
        SystemParameterResolver resolver =
                org.mockito.Mockito.mock(SystemParameterResolver.class);
        org.mockito.Mockito.when(resolver.resolve(
                        "cvm.ai.reachability.git.cache-dir"))
                .thenReturn(Optional.of(alternativerCache.toString()));
        JGitGitCheckoutAdapter resolverAware = new JGitGitCheckoutAdapter(
                cacheRoot.toString(), 72, Optional.of(resolver));

        Path workdir = resolverAware.checkout(
                bareRepo.toUri().toString(), "master", commitSha);

        assertThat(workdir).startsWith(alternativerCache);
    }
}
