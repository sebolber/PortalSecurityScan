package com.ahs.cvm.integration.osv;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OsvJsonlMirrorTest {

    @TempDir
    Path tempDir;

    private Path writeJsonl(String content) throws Exception {
        Path file = tempDir.resolve("osv.jsonl");
        Files.writeString(file, content);
        return file;
    }

    @Test
    @DisplayName("OsvJsonlMirror: leere Datei -> keine Treffer")
    void leereDatei() throws Exception {
        Path file = writeJsonl("");
        OsvJsonlMirror mirror = new OsvJsonlMirror(file);
        mirror.reload();

        assertThat(mirror.size()).isZero();
        assertThat(mirror.findCveIdsForPurls(List.of("pkg:maven/foo/bar")))
                .isEmpty();
    }

    @Test
    @DisplayName("OsvJsonlMirror: Advisory mit aliases=CVE-x + affected.package.purl wird indiziert")
    void advisoryMitAlias() throws Exception {
        Path file = writeJsonl("""
                {"id":"GHSA-abcd","aliases":["CVE-2025-48924"],"affected":[{"package":{"purl":"pkg:maven/org.foo/bar@1.0"}}]}
                """);
        OsvJsonlMirror mirror = new OsvJsonlMirror(file);
        mirror.reload();

        Map<String, List<String>> result =
                mirror.findCveIdsForPurls(List.of("pkg:maven/org.foo/bar@1.0"));
        assertThat(result).containsOnlyKeys("pkg:maven/org.foo/bar@1.0");
        assertThat(result.get("pkg:maven/org.foo/bar@1.0"))
                .containsExactly("CVE-2025-48924");
    }

    @Test
    @DisplayName("OsvJsonlMirror: Advisory mit direkter CVE-ID im id-Feld wird erkannt")
    void advisoryMitDirekterCve() throws Exception {
        Path file = writeJsonl("""
                {"id":"CVE-2017-18640","affected":[{"package":{"purl":"pkg:pypi/pyyaml"}}]}
                """);
        OsvJsonlMirror mirror = new OsvJsonlMirror(file);
        mirror.reload();

        assertThat(mirror.findCveIdsForPurls(List.of("pkg:pypi/pyyaml")))
                .containsEntry("pkg:pypi/pyyaml", List.of("CVE-2017-18640"));
    }

    @Test
    @DisplayName("OsvJsonlMirror: mehrere Advisories pro PURL -> Set ohne Duplikate")
    void mehrereAdvisoriesProPurl() throws Exception {
        Path file = writeJsonl(
                "{\"id\":\"GHSA-1\",\"aliases\":[\"CVE-2025-1\"],\"affected\":[{\"package\":{\"purl\":\"pkg:npm/left-pad\"}}]}\n"
                        + "{\"id\":\"GHSA-2\",\"aliases\":[\"CVE-2025-2\"],\"affected\":[{\"package\":{\"purl\":\"pkg:npm/left-pad\"}}]}\n"
                        + "{\"id\":\"GHSA-3\",\"aliases\":[\"CVE-2025-1\"],\"affected\":[{\"package\":{\"purl\":\"pkg:npm/left-pad\"}}]}\n");
        OsvJsonlMirror mirror = new OsvJsonlMirror(file);
        mirror.reload();

        List<String> cves = mirror.findCveIdsForPurls(List.of("pkg:npm/left-pad"))
                .get("pkg:npm/left-pad");
        assertThat(cves).containsExactlyInAnyOrder("CVE-2025-1", "CVE-2025-2");
    }

    @Test
    @DisplayName("OsvJsonlMirror: reload() spiegelt Datei-Aenderungen wider")
    void reloadSpiegeltAenderung() throws Exception {
        Path file = writeJsonl(
                "{\"id\":\"GHSA-a\",\"aliases\":[\"CVE-2025-1\"],\"affected\":[{\"package\":{\"purl\":\"pkg:npm/alpha\"}}]}\n");
        OsvJsonlMirror mirror = new OsvJsonlMirror(file);
        mirror.reload();
        assertThat(mirror.size()).isEqualTo(1);

        Files.writeString(file,
                "{\"id\":\"GHSA-a\",\"aliases\":[\"CVE-2025-1\"],\"affected\":[{\"package\":{\"purl\":\"pkg:npm/alpha\"}}]}\n"
                        + "{\"id\":\"GHSA-b\",\"aliases\":[\"CVE-2025-2\"],\"affected\":[{\"package\":{\"purl\":\"pkg:npm/beta\"}}]}\n");
        mirror.reload();
        assertThat(mirror.size()).isEqualTo(2);
        assertThat(mirror.findCveIdsForPurls(List.of("pkg:npm/beta"))
                .get("pkg:npm/beta"))
                .containsExactly("CVE-2025-2");
    }

    @Test
    @DisplayName("OsvJsonlMirror: defekte Zeile wird ignoriert, Rest weiter indiziert")
    void defekteZeileIgnoriert() throws Exception {
        Path file = writeJsonl(
                "das-ist-kein-json\n"
                        + "{\"id\":\"GHSA-ok\",\"aliases\":[\"CVE-2025-9\"],\"affected\":[{\"package\":{\"purl\":\"pkg:npm/ok\"}}]}\n");
        OsvJsonlMirror mirror = new OsvJsonlMirror(file);
        mirror.reload();

        assertThat(mirror.size()).isEqualTo(1);
        assertThat(mirror.findCveIdsForPurls(List.of("pkg:npm/ok")))
                .containsEntry("pkg:npm/ok", List.of("CVE-2025-9"));
    }

    @Test
    @DisplayName("OsvJsonlMirror: nicht existierende Datei -> leerer Index, keine Exception")
    void nichtExistierendeDatei() {
        Path ghost = tempDir.resolve("ghost.jsonl");
        OsvJsonlMirror mirror = new OsvJsonlMirror(ghost);
        mirror.reload();

        assertThat(mirror.size()).isZero();
        assertThat(mirror.findCveIdsForPurls(List.of("pkg:any"))).isEmpty();
    }
}
