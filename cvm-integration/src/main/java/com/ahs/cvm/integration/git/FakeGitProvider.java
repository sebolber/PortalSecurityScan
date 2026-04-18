package com.ahs.cvm.integration.git;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default-Bean: liefert Daten, die der Test vorher ueber
 * {@link #stub(String, String, ReleaseNotes, List)} hinterlegt hat.
 * Wird durch den realen {@code GitHubApiProvider} verdraengt, sobald
 * dieser aktiv ist.
 */
@Component
@ConditionalOnMissingBean(name = "githubApiProvider")
public class FakeGitProvider implements GitProviderPort {

    private final Map<String, ReleaseNotes> notes = new HashMap<>();
    private final Map<String, List<CommitSummary>> commits = new HashMap<>();

    public void stub(String repoUrl, String toTag,
            ReleaseNotes note, List<CommitSummary> commitRange) {
        notes.put(key(repoUrl, toTag), note);
        commits.put(key(repoUrl, toTag), List.copyOf(commitRange));
    }

    public void reset() {
        notes.clear();
        commits.clear();
    }

    @Override
    public Optional<ReleaseNotes> releaseNotes(String repoUrl, String tag) {
        return Optional.ofNullable(notes.get(key(repoUrl, tag)));
    }

    @Override
    public List<CommitSummary> compare(String repoUrl, String fromTag, String toTag) {
        // Range-Key ist toTag (kleinste Stabilitaet fuer Tests).
        return new ArrayList<>(commits.getOrDefault(key(repoUrl, toTag), List.of()));
    }

    private static String key(String repoUrl, String tag) {
        return repoUrl + "#" + tag;
    }
}
