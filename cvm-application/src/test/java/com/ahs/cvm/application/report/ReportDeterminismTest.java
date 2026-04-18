package com.ahs.cvm.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.persistence.report.GeneratedReport;
import com.ahs.cvm.persistence.report.GeneratedReportRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Determinismus-Goldmaster im Kleinen: zwei Erzeugungen mit identischem
 * Input muessen byte-gleiche PDF-Bytes liefern. Der SHA-256-Hash wird
 * als zweiter Anker verglichen.
 */
class ReportDeterminismTest {

    @Test
    @DisplayName("Report: gleiche Eingabe zweimal liefert byte-gleiches PDF und gleichen SHA-256")
    void identischesPdf() {
        HardeningReportDataLoader loader = mock(HardeningReportDataLoader.class);
        GeneratedReportRepository repository = mock(GeneratedReportRepository.class);
        given(loader.load(any())).willReturn(HardeningReportFixtures.data());
        AtomicInteger counter = new AtomicInteger();
        given(repository.save(any(GeneratedReport.class))).willAnswer(inv -> {
            GeneratedReport r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(java.util.UUID.nameUUIDFromBytes(
                        ("seed-" + counter.incrementAndGet()).getBytes()));
            }
            return r;
        });

        Clock clock = Clock.fixed(
                Instant.parse("2026-04-18T10:00:00Z"), ZoneOffset.UTC);
        ReportGeneratorService service = new ReportGeneratorService(
                loader,
                new HardeningReportTemplateRenderer(
                        new ReportConfig().reportTemplateEngine()),
                new HardeningReportPdfRenderer(),
                repository,
                clock);

        GeneratedReportView a =
                service.generateHardeningReport(HardeningReportFixtures.input());
        GeneratedReportView b =
                service.generateHardeningReport(HardeningReportFixtures.input());

        assertThat(a.pdfBytes()).isEqualTo(b.pdfBytes());
        assertThat(a.sha256()).isEqualTo(b.sha256());
    }
}
