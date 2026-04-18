package com.ahs.cvm.llm.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromptTemplateLoaderTest {

    private final PromptTemplateLoader loader = new PromptTemplateLoader();

    @Test
    @DisplayName("PromptLoader: laedt assessment.propose und rendert Variablen korrekt")
    void laedtUndRendert() {
        PromptTemplate t = loader.load("assessment.propose");
        assertThat(t.id()).isEqualTo("assessment.propose");
        assertThat(t.version()).isEqualTo("v1");
        String user = t.renderUser(Map.of(
                "cveKey", "CVE-2025-48924",
                "produkt", "PortalCore-Test",
                "produktVersion", "1.14.2-test",
                "umgebung", "REF-TEST",
                "profilAuszug", "schemaVersion: 1"));
        assertThat(user).contains("CVE-2025-48924");
        assertThat(user).contains("REF-TEST");
    }

    @Test
    @DisplayName("PromptLoader: fehlendes Template wirft IllegalArgumentException")
    void fehltTemplate() {
        assertThatThrownBy(() -> loader.load("gibt-es-nicht"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("PromptLoader: fehlende Variable bei Substitution wirft IllegalArgumentException")
    void fehltVariable() {
        PromptTemplate t = loader.load("assessment.propose");
        assertThatThrownBy(() -> t.renderUser(Map.of("cveKey", "CVE-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template-Variable fehlt");
    }

    @Test
    @DisplayName("PromptLoader: parse trennt version / system / user korrekt")
    void parseStrukturiert() {
        String quelle = """
                #version: v9
                #system:
                system line
                #user:
                user line ${x}
                """;
        PromptTemplate t = PromptTemplateLoader.parse("dummy", quelle);
        assertThat(t.version()).isEqualTo("v9");
        assertThat(t.systemPrompt()).isEqualTo("system line");
        assertThat(t.renderUser(Map.of("x", "Y")))
                .isEqualTo("user line Y");
    }
}
