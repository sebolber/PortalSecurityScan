package com.ahs.cvm.application.vex;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VexImporterTest {

    private final VexImporter importer = new VexImporter();

    @Test
    @DisplayName("VEX-Import: Happy-Path parst vulnerabilities aus CycloneDX")
    void happyPath() {
        String doc = """
                {
                  "bomFormat":"CycloneDX",
                  "specVersion":"1.6",
                  "vulnerabilities":[
                    {"id":"CVE-2025-48924",
                     "analysis":{"state":"not_affected",
                                 "justification":"component_not_present",
                                 "detail":"nicht eingebunden"},
                     "affects":[{"ref":"pkg:maven/org.x/lib@1.2.3"}]}
                  ]
                }""";

        VexImporter.Parsed p = importer.parse(doc);

        assertThat(p.errors()).isEmpty();
        assertThat(p.statements()).hasSize(1);
        assertThat(p.statements().get(0).status()).isEqualTo(VexStatus.NOT_AFFECTED);
        assertThat(p.statements().get(0).productPurl())
                .isEqualTo("pkg:maven/org.x/lib@1.2.3");
    }

    @Test
    @DisplayName("VEX-Import: unbekannter state -> Warning, Statement wird uebersprungen")
    void unbekannterState() {
        String doc = """
                {"bomFormat":"CycloneDX","specVersion":"1.6",
                 "vulnerabilities":[{"id":"CVE-X",
                   "analysis":{"state":"FOOBAR"}}]}""";

        VexImporter.Parsed p = importer.parse(doc);

        assertThat(p.errors()).isEmpty();
        assertThat(p.statements()).isEmpty();
        assertThat(p.warnings()).anyMatch(w -> w.contains("FOOBAR"));
    }

    @Test
    @DisplayName("VEX-Import: Nicht-CycloneDX-Format wird abgelehnt")
    void nichtCycloneDx() {
        String doc = """
                {"bomFormat":"SPDX","vulnerabilities":[]}""";

        VexImporter.Parsed p = importer.parse(doc);

        assertThat(p.errors()).anyMatch(e -> e.contains("CycloneDX"));
    }

    @Test
    @DisplayName("VEX-Import: kaputtes JSON -> Fehler, keine Exception")
    void kaputt() {
        VexImporter.Parsed p = importer.parse("{nicht-json");

        assertThat(p.errors()).isNotEmpty();
        assertThat(p.statements()).isEmpty();
    }
}
