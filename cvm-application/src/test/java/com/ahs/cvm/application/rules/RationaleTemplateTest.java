package com.ahs.cvm.application.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RationaleTemplateTest {

    private final RationaleTemplateInterpolator interpolator =
            new RationaleTemplateInterpolator();

    private RuleEvaluationContext kontext() throws Exception {
        YAMLMapper yaml = new YAMLMapper();
        JsonNode profile = yaml.readTree(
                """
                schemaVersion: 1
                umgebung:
                  key: REF-TEST
                  stage: REF
                architecture:
                  windows_hosts: false
                """);
        return new RuleEvaluationContext(
                new RuleEvaluationContext.CveSnapshot(
                        UUID.randomUUID(),
                        "CVE-2017-18640",
                        "SnakeYAML billion laughs",
                        List.of("CWE-776"),
                        true,
                        new BigDecimal("0.81"),
                        new BigDecimal("7.5")),
                profile,
                new RuleEvaluationContext.ComponentSnapshot(
                        "maven", "snakeyaml", "1.19"),
                new RuleEvaluationContext.FindingSnapshot(
                        UUID.randomUUID(), Instant.now()));
    }

    @Test
    @DisplayName("Template: einfache Tokens werden ersetzt")
    void einfacheToken() throws Exception {
        String text = interpolator.interpolate(
                "CVE {cve.id} auf Komponente {component.name} {component.version}",
                kontext());
        assertThat(text).isEqualTo("CVE CVE-2017-18640 auf Komponente snakeyaml 1.19");
    }

    @Test
    @DisplayName("Template: profile-Pfad wird ausgewertet")
    void profileToken() throws Exception {
        String text = interpolator.interpolate(
                "Keine Windows-Plattform (windows_hosts={profile.architecture.windows_hosts})",
                kontext());
        assertThat(text).isEqualTo(
                "Keine Windows-Plattform (windows_hosts=false)");
    }

    @Test
    @DisplayName("Template: unbekanntes Token bleibt als Text und wird markiert")
    void unbekanntesToken() throws Exception {
        String text = interpolator.interpolate(
                "Wert {cve.nichtDa} hier",
                kontext());
        assertThat(text).contains("{cve.nichtDa}");
    }
}
