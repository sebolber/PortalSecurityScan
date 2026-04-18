package com.ahs.cvm.application.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HardeningReportPdfRendererTest {

    private static final String MINIMAL_HTML = """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"/><title>T</title></head>
            <body>
                <h1>CVM-TEST-MARKER</h1>
                <p>Inhalt PortalCore-Test / 1.14.2-test / REF-TEST.</p>
            </body></html>
            """;

    private final HardeningReportPdfRenderer renderer = new HardeningReportPdfRenderer();

    @Test
    @DisplayName("PDF-Header beginnt mit %PDF, Laenge > 0")
    void pdfHeader() {
        byte[] pdf = renderer.render(
                MINIMAL_HTML,
                Instant.parse("2026-04-18T10:00:00Z"),
                "deadbeef");

        assertThat(pdf).isNotEmpty();
        String header = new String(pdf, 0, 5);
        assertThat(header).startsWith("%PDF");
    }

    @Test
    @DisplayName("PDF kann ueber PDFBox geladen werden und enthaelt die Marker")
    void pdfEnthaeltMarker() throws Exception {
        byte[] pdf = renderer.render(
                MINIMAL_HTML,
                Instant.parse("2026-04-18T10:00:00Z"),
                "deadbeef");

        try (PDDocument doc = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("CVM-TEST-MARKER");
            assertThat(text).contains("PortalCore-Test");
            assertThat(text).contains("1.14.2-test");
            assertThat(doc.getDocumentInformation().getProducer())
                    .isEqualTo("CVM Report 1.0");
        }
    }

    @Test
    @DisplayName("Zweimalige Erzeugung mit gleichen Eingaben liefert byte-gleiches PDF")
    void determinismus() {
        Instant t = Instant.parse("2026-04-18T10:00:00Z");
        byte[] a = renderer.render(MINIMAL_HTML, t, "deadbeef");
        byte[] b = renderer.render(MINIMAL_HTML, t, "deadbeef");
        assertThat(a).isEqualTo(b);
    }
}
