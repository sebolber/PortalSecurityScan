package com.ahs.cvm.application.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlertTemplateRendererTest {

    private final AlertTemplateRenderer renderer = new AlertTemplateRenderer();

    @Test
    @DisplayName("KEV-Hit-Template ersetzt Platzhalter und HTML-escaped Inhalt")
    void kevHitWirdGerendert() {
        String html = renderer.render("alert-kev-hit", Map.of(
                "subjectPrefix", "[CVM]",
                "cveKey", "CVE-2025-<1>",
                "severity", "CRITICAL",
                "produktVersion", "1.14.2-test",
                "cveUrl", "https://example.test/cve"));

        assertThat(html).contains("CVE-2025-&lt;1&gt;");
        assertThat(html).contains("CRITICAL");
        assertThat(html).contains("1.14.2-test");
        assertThat(html).doesNotContain("{{");
    }

    @Test
    @DisplayName("Unbekanntes Template wirft IllegalArgumentException")
    void unbekanntesTemplateWirft() {
        assertThatThrownBy(() -> renderer.render("gibts-nicht", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toPlainText entfernt HTML-Tags und decodiert Entities")
    void toPlainTextEntferntTags() {
        String html = "<p>Hallo <strong>Welt</strong></p><br/>2&nbsp;Zeilen";
        String text = renderer.toPlainText(html);
        assertThat(text).contains("Hallo Welt");
        assertThat(text).doesNotContain("<");
        assertThat(text).doesNotContain("&nbsp;");
    }
}
