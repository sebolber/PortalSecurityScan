package com.ahs.cvm.application.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContextProfileYamlParserTest {

    private final ContextProfileYamlParser parser = new ContextProfileYamlParser();

    @Test
    @DisplayName("YAML-Parser: vollstaendiges gueltiges Profil wird akzeptiert")
    void gueltigesProfil() {
        String yaml =
                """
                schemaVersion: 1
                umgebung:
                  key: REF-TEST
                  stage: REF
                architecture:
                  windows_hosts: false
                  linux_hosts: true
                  kubernetes: true
                network:
                  internet_exposure: false
                  customer_access: true
                hardening:
                  fips_mode: false
                compliance:
                  frameworks:
                    - ISO27001
                """;

        ParsedProfile parsed = parser.parse(yaml);

        assertThat(parsed.tree().get("umgebung").get("key").asText()).isEqualTo("REF-TEST");
        assertThat(parsed.tree().get("architecture").get("kubernetes").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("YAML-Parser: fehlendes Pflichtfeld umgebung.key wirft ProfileValidationException")
    void pflichtfeldFehlt() {
        String yaml =
                """
                schemaVersion: 1
                umgebung:
                  stage: REF
                """;

        assertThatThrownBy(() -> parser.parse(yaml))
                .isInstanceOf(ProfileValidationException.class)
                .hasMessageContaining("umgebung.key");
    }

    @Test
    @DisplayName("YAML-Parser: falscher Typ (stage als Zahl) wirft ProfileValidationException mit deutscher Meldung")
    void falscherTyp() {
        String yaml =
                """
                schemaVersion: 1
                umgebung:
                  key: REF-TEST
                  stage: 1
                """;

        assertThatThrownBy(() -> parser.parse(yaml))
                .isInstanceOf(ProfileValidationException.class)
                .hasMessageContaining("stage");
    }

    @Test
    @DisplayName("YAML-Parser: ungueltiges YAML (Syntaxfehler) wirft ProfileValidationException")
    void ungueltigesYaml() {
        String kaputt = "schemaVersion: 1\nfoo: {bar: : baz";

        assertThatThrownBy(() -> parser.parse(kaputt))
                .isInstanceOf(ProfileValidationException.class)
                .hasMessageContaining("YAML");
    }

    @Test
    @DisplayName("YAML-Parser: unbekannter stage-Wert wird abgelehnt")
    void stageOutOfEnum() {
        String yaml =
                """
                schemaVersion: 1
                umgebung:
                  key: REF-TEST
                  stage: CLOUD
                """;

        assertThatThrownBy(() -> parser.parse(yaml))
                .isInstanceOf(ProfileValidationException.class)
                .hasMessageContaining("stage");
    }
}
