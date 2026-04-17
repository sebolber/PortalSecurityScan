package com.ahs.cvm.application.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Herzstueck der Profil-Versionierung. TDD zuerst &mdash; der Diff-Builder
 * muss deterministische, typisierte Aenderungen liefern, damit der
 * AssessmentReviewMarker verlaesslich arbeitet.
 */
class ProfileDiffBuilderTest {

    private final YAMLMapper yaml = new YAMLMapper();
    private final ProfileDiffBuilder builder = new ProfileDiffBuilder();

    @Test
    @DisplayName("Diff: identische Profile liefern leere Liste")
    void identischLeer() throws Exception {
        JsonNode alt = parse("a: 1\nb: true");
        JsonNode neu = parse("a: 1\nb: true");

        assertThat(builder.diff(alt, neu)).isEmpty();
    }

    @Test
    @DisplayName("Diff: boolean-Flip erzeugt CHANGED mit Pfad")
    void booleanFlip() throws Exception {
        JsonNode alt = parse("architecture:\n  windows_hosts: false");
        JsonNode neu = parse("architecture:\n  windows_hosts: true");

        List<ProfileFieldDiff> diffs = builder.diff(alt, neu);

        assertThat(diffs).hasSize(1);
        ProfileFieldDiff d = diffs.get(0);
        assertThat(d.path()).isEqualTo("architecture.windows_hosts");
        assertThat(d.changeType()).isEqualTo(ProfileFieldDiff.ChangeType.CHANGED);
        assertThat(d.altWert().asBoolean()).isFalse();
        assertThat(d.neuWert().asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Diff: hinzugefuegtes Feld erzeugt CREATED")
    void feldHinzugefuegt() throws Exception {
        JsonNode alt = parse("architecture:\n  linux_hosts: true");
        JsonNode neu = parse("architecture:\n  linux_hosts: true\n  kubernetes: true");

        List<ProfileFieldDiff> diffs = builder.diff(alt, neu);

        assertThat(diffs).hasSize(1);
        assertThat(diffs.get(0).path()).isEqualTo("architecture.kubernetes");
        assertThat(diffs.get(0).changeType()).isEqualTo(ProfileFieldDiff.ChangeType.CREATED);
        assertThat(diffs.get(0).altWert()).isNull();
        assertThat(diffs.get(0).neuWert().asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Diff: entferntes Feld erzeugt REMOVED")
    void feldEntfernt() throws Exception {
        JsonNode alt = parse("network:\n  customer_access: true\n  internet_exposure: true");
        JsonNode neu = parse("network:\n  customer_access: true");

        List<ProfileFieldDiff> diffs = builder.diff(alt, neu);

        assertThat(diffs).hasSize(1);
        assertThat(diffs.get(0).path()).isEqualTo("network.internet_exposure");
        assertThat(diffs.get(0).changeType()).isEqualTo(ProfileFieldDiff.ChangeType.REMOVED);
        assertThat(diffs.get(0).altWert().asBoolean()).isTrue();
        assertThat(diffs.get(0).neuWert()).isNull();
    }

    @Test
    @DisplayName("Diff: tief verschachtelte Struktur liefert korrekte Punkt-Pfade")
    void tiefVerschachtelt() throws Exception {
        JsonNode alt = parse("""
                umgebung:
                  key: REF-TEST
                  stage: REF
                architecture:
                  windows_hosts: false
                  linux_hosts: true
                network:
                  internet_exposure: false
                """);
        JsonNode neu = parse("""
                umgebung:
                  key: REF-TEST
                  stage: REF
                architecture:
                  windows_hosts: true
                  linux_hosts: true
                  kubernetes: true
                network:
                  internet_exposure: true
                """);

        List<ProfileFieldDiff> diffs = builder.diff(alt, neu);

        assertThat(diffs).extracting(ProfileFieldDiff::path)
                .containsExactly(
                        "architecture.kubernetes",
                        "architecture.windows_hosts",
                        "network.internet_exposure");
    }

    @Test
    @DisplayName("Diff: Array-Aenderung liefert index-basierten Pfad")
    void arrayDiff() throws Exception {
        JsonNode alt = parse("""
                compliance:
                  frameworks:
                    - ISO27001
                    - BSI
                """);
        JsonNode neu = parse("""
                compliance:
                  frameworks:
                    - ISO27001
                    - DSGVO
                """);

        List<ProfileFieldDiff> diffs = builder.diff(alt, neu);

        assertThat(diffs).hasSize(1);
        assertThat(diffs.get(0).path()).isEqualTo("compliance.frameworks[1]");
        assertThat(diffs.get(0).changeType()).isEqualTo(ProfileFieldDiff.ChangeType.CHANGED);
    }

    @Test
    @DisplayName("Diff: Reihenfolge der Aenderungen ist deterministisch (alphabetisch nach Pfad)")
    void reihenfolgeDeterministisch() throws Exception {
        JsonNode alt = parse("z: 1\nm: 1\na: 1");
        JsonNode neu = parse("z: 2\nm: 2\na: 2");

        List<ProfileFieldDiff> diffs = builder.diff(alt, neu);

        assertThat(diffs).extracting(ProfileFieldDiff::path)
                .containsExactly("a", "m", "z");
    }

    private JsonNode parse(String yamlText) throws Exception {
        return yaml.readTree(yamlText);
    }
}
