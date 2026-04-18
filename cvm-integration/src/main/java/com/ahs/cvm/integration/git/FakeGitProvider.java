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
    private final List<MergeRequestComment> postedComments = new ArrayList<>();
    private boolean simulatePostFailure;

    public void stub(String repoUrl, String toTag,
            ReleaseNotes note, List<CommitSummary> commitRange) {
        notes.put(key(repoUrl, toTag), note);
        commits.put(key(repoUrl, toTag), List.copyOf(commitRange));
    }

    public void reset() {
        notes.clear();
        commits.clear();
        postedComments.clear();
        simulatePostFailure = false;
    }

    public void simulatePostFailure(boolean simulate) {
        this.simulatePostFailure = simulate;
    }

    public List<MergeRequestComment> postedComments() {
        return List.copyOf(postedComments);
    }

    @Override
    public boolean postMergeRequestComment(String repoUrl,
            String mergeRequestId, String body) {
        if (simulatePostFailure) {
            throw new RuntimeException("simulated post failure");
        }
        postedComments.add(new MergeRequestComment(repoUrl, mergeRequestId, body));
        return true;
    }

    public record MergeRequestComment(String repoUrl, String mergeRequestId,
            String body) {}

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
