package com.ahs.cvm.application.report;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HardeningReportTemplateRendererTest {

    private final HardeningReportTemplateRenderer renderer =
            new HardeningReportTemplateRenderer(
                    new ReportConfig().reportTemplateEngine());

    @Test
    @DisplayName("Template enthaelt Kopfdaten, Ampel und Kennzahlen-Zeilen")
    void rendert_kopf_und_kennzahlen() {
        String html = renderer.render(HardeningReportFixtures.data());

        assertThat(html).contains("Hardening-Report");
        assertThat(html).contains("PortalCore-Test");
        assertThat(html).contains("1.14.2-test");
        assertThat(html).contains("REF-TEST");
        assertThat(html).contains("a.admin@ahs.test");
        // Gesamteinstufungs-Ampel
        assertThat(html).contains("ampel-MEDIUM");
        // Alle Kategorien aus Kennzahlen-Tabelle
        assertThat(html).contains("Plattform");
        assertThat(html).contains("Docker");
        assertThat(html).contains("Java");
        assertThat(html).contains("NodeJS");
        assertThat(html).contains("Python");
    }

    @Test
    @DisplayName("Template listet CVEs mit Link auf NVD und Severity-Ampel")
    void rendert_cveListe() {
        String html = renderer.render(HardeningReportFixtures.data());

        assertThat(html).contains("CVE-2017-18640");
        assertThat(html).contains("CVE-2025-48924");
        assertThat(html).contains("https://nvd.nist.gov/vuln/detail/CVE-2025-48924");
        assertThat(html).contains("ampel-CRITICAL");
    }

    @Test
    @DisplayName("Template zeigt offene Punkte und Anhang (Profil + Regeln + VEX-Platzhalter)")
    void rendert_offenePunkteUndAnhang() {
        String html = renderer.render(HardeningReportFixtures.data());

        assertThat(html).contains("CVE-2026-22610");
        assertThat(html).contains("PROPOSED");
        assertThat(html).contains("PC-2025-01");
        assertThat(html).contains("Plattform-Kritisch");
        assertThat(html).contains("Platzhalter");
    }
}
