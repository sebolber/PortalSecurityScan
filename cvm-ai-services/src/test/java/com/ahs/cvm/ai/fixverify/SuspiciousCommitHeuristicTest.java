package com.ahs.cvm.ai.fixverify;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.ai.fixverify.SuspiciousCommitHeuristic.Verdict;
import com.ahs.cvm.integration.git.GitProviderPort.CommitSummary;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SuspiciousCommitHeuristicTest {

    private final SuspiciousCommitHeuristic heuristic = new SuspiciousCommitHeuristic();

    private CommitSummary commit(String message, List<String> files) {
        return new CommitSummary("sha1", message,
                "https://example/commit/sha1", "a@x", Instant.now(), files);
    }

    @Test
    @DisplayName("Heuristik: CVE-ID in Message -> verdaechtig")
    void cveId() {
        Verdict v = heuristic.classify(
                commit("fix: addresses CVE-2025-48924", List.of()),
                "Parser.parse(String)", "CVE-2025-48924");
        assertThat(v.suspicious()).isTrue();
        assertThat(v.reason()).contains("CVE-");
    }

    @Test
    @DisplayName("Heuristik: GHSA-ID -> verdaechtig")
    void ghsa() {
        Verdict v = heuristic.classify(
                commit("sec: cf GHSA-abcd-1234-wxyz", List.of()),
                null, null);
        assertThat(v.suspicious()).isTrue();
        assertThat(v.reason()).contains("GHSA");
    }

    @Test
    @DisplayName("Heuristik: Keyword security/vulnerability -> verdaechtig")
    void keyword() {
        Verdict v = heuristic.classify(
                commit("fix: prevent XXE in parser", List.of()),
                null, null);
        assertThat(v.suspicious()).isTrue();
        assertThat(v.reason()).contains("xxe");
    }

    @Test
    @DisplayName("Heuristik: Datei-Treffer fuer vulnerable Symbol -> verdaechtig")
    void fileTreffer() {
        Verdict v = heuristic.classify(
                commit("chore: tidy up helpers",
                        List.of("src/main/java/com/x/Parser.java")),
                "com.x.Parser.parse(String)", "CVE-X");
        assertThat(v.suspicious()).isTrue();
        assertThat(v.reason()).contains("Datei");
    }

    @Test
    @DisplayName("Heuristik: reiner Refactor ohne Keyword -> nicht verdaechtig")
    void refactorNichtVerdaechtig() {
        Verdict v = heuristic.classify(
                commit("chore: reformat and rename helpers", List.of("README.md")),
                "Parser.parse", "CVE-X");
        assertThat(v.suspicious()).isFalse();
    }

    @Test
    @DisplayName("Heuristik: fileBasisName extrahiert Klassennamen")
    void fileBasis() {
        assertThat(SuspiciousCommitHeuristic.fileBasisName(
                "com.example.Parser.parse(String)")).isEqualTo("parser");
        assertThat(SuspiciousCommitHeuristic.fileBasisName(
                "JsonReader.read")).isEqualTo("jsonreader");
    }
}
