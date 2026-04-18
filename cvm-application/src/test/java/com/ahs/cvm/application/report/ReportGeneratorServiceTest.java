package com.ahs.cvm.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.persistence.report.GeneratedReport;
import com.ahs.cvm.persistence.report.GeneratedReportRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportGeneratorServiceTest {

    private HardeningReportDataLoader loader;
    private GeneratedReportRepository repository;
    private Clock clock;
    private ReportGeneratorService service;

    @BeforeEach
    void setUp() {
        loader = mock(HardeningReportDataLoader.class);
        repository = mock(GeneratedReportRepository.class);
        clock = Clock.fixed(
                Instant.parse("2026-04-18T10:00:00Z"), ZoneOffset.UTC);

        HardeningReportTemplateRenderer templateRenderer =
                new HardeningReportTemplateRenderer(
                        new ReportConfig().reportTemplateEngine());
        HardeningReportPdfRenderer pdfRenderer = new HardeningReportPdfRenderer();
        given(loader.load(any())).willReturn(HardeningReportFixtures.data());
        given(repository.save(any(GeneratedReport.class)))
                .willAnswer(inv -> {
                    GeneratedReport r = inv.getArgument(0);
                    if (r.getId() == null) {
                        r.setId(UUID.fromString(
                                "11111111-1111-1111-1111-111111111111"));
                    }
                    return r;
                });
        service = new ReportGeneratorService(
                loader, templateRenderer, pdfRenderer, repository, clock);
    }

    @Test
    @DisplayName("Report: erzeugt PDF, berechnet SHA-256 und persistiert den Datensatz")
    void generiertReport() throws Exception {
        GeneratedReportView view =
                service.generateHardeningReport(HardeningReportFixtures.input());

        assertThat(view.reportId()).isNotNull();
        assertThat(view.gesamteinstufung()).isEqualTo(AhsSeverity.MEDIUM);
        assertThat(view.erzeugtVon()).isEqualTo("a.admin@ahs.test");
        assertThat(view.sha256()).hasSize(64);
        assertThat(view.pdfBytes()).isNotEmpty();

        // PDF laedt und enthaelt die erwarteten Marker-Strings.
        try (PDDocument doc = PDDocument.load(view.pdfBytes())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Hardening-Report");
            assertThat(text).contains("PortalCore-Test");
            assertThat(text).contains("CVE-2025-48924");
            assertThat(text).contains("PC-2025-01");
            assertThat(doc.getDocumentInformation().getCreationDate()
                            .toInstant())
                    .isEqualTo(clock.instant());
        }

        // SHA-256 entspricht dem, was der Service selbst berechnen wuerde.
        String erwartet = ReportGeneratorService.sha256(view.pdfBytes());
        assertThat(view.sha256()).isEqualTo(erwartet);
    }

    @Test
    @DisplayName("Report: erzwingt Pflichtfelder (productVersionId, erzeugtVon)")
    void pflichtfelder() {
        HardeningReportInput ohneProduktVersion = new HardeningReportInput(
                null,
                HardeningReportFixtures.ENVIRONMENT_ID,
                AhsSeverity.MEDIUM,
                null,
                "a.admin@ahs.test",
                HardeningReportFixtures.STICHTAG);
        assertThatThrownBy(() -> service.generateHardeningReport(ohneProduktVersion))
                .isInstanceOf(IllegalArgumentException.class);

        HardeningReportInput ohneErzeuger = new HardeningReportInput(
                HardeningReportFixtures.PRODUCT_VERSION_ID,
                HardeningReportFixtures.ENVIRONMENT_ID,
                AhsSeverity.MEDIUM,
                null,
                " ",
                HardeningReportFixtures.STICHTAG);
        assertThatThrownBy(() -> service.generateHardeningReport(ohneErzeuger))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Report: findById wirft ReportNotFoundException bei unbekannter ID")
    void findByIdUnbekannt() {
        UUID unbekannt = UUID.randomUUID();
        assertThatThrownBy(() -> service.findById(unbekannt))
                .isInstanceOf(ReportNotFoundException.class);
    }
}
