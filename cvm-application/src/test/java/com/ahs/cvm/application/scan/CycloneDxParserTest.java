package com.ahs.cvm.application.scan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class CycloneDxParserTest {

    private final CycloneDxParser parser = new CycloneDxParser();

    @Test
    @DisplayName("Parser: liest kleine CycloneDX-SBOM mit 5 Komponenten und 2 CVEs")
    void parserHappyPath() throws IOException {
        byte[] raw = new ClassPathResource("fixtures/cyclonedx/klein.json")
                .getContentAsByteArray();

        CycloneDxBom bom = parser.parse(raw);

        assertThat(bom.bomFormat()).isEqualTo("CycloneDX");
        assertThat(bom.specVersion()).isEqualTo("1.6");
        assertThat(bom.components()).hasSize(5);
        assertThat(bom.vulnerabilities()).hasSize(2);
        assertThat(bom.vulnerabilities().get(0).id()).isEqualTo("CVE-2017-18640");
    }

    @Test
    @DisplayName("Parser: wirft SbomParseException bei invalidem JSON")
    void parserWirftBeiInvalidemJson() {
        byte[] raw = "kein-json".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> parser.parse(raw))
                .isInstanceOf(SbomParseException.class);
    }

    @Test
    @DisplayName("Parser: wirft SbomSchemaException bei unpassendem bomFormat")
    void parserWirftBeiSpdx() throws IOException {
        byte[] raw = new ClassPathResource("fixtures/cyclonedx/kein-format.json")
                .getContentAsByteArray();

        assertThatThrownBy(() -> parser.parse(raw))
                .isInstanceOf(SbomSchemaException.class)
                .hasMessageContaining("bomFormat");
    }

    @Test
    @DisplayName("Parser: wirft SbomSchemaException bei leerer Komponenten-Liste")
    void parserWirftBeiLeerenKomponenten() {
        String minimal = """
                {
                  "bomFormat": "CycloneDX",
                  "specVersion": "1.6",
                  "components": []
                }
                """;

        assertThatThrownBy(() -> parser.parse(minimal))
                .isInstanceOf(SbomSchemaException.class)
                .hasMessageContaining("Komponenten");
    }
}
