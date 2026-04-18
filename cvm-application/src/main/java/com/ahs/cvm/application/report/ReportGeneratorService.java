package com.ahs.cvm.application.report;

import com.ahs.cvm.persistence.report.GeneratedReport;
import com.ahs.cvm.persistence.report.GeneratedReportRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestriert den kompletten Erzeugungsweg eines Hardening-Reports
 * (CVM-19):
 *
 * <ol>
 *   <li>{@link HardeningReportDataLoader} baut das Read-Model.</li>
 *   <li>{@link HardeningReportTemplateRenderer} rendert das HTML.</li>
 *   <li>{@link HardeningReportPdfRenderer} wandelt HTML -> PDF mit
 *       deterministischen Metadaten.</li>
 *   <li>SHA-256 wird ueber die PDF-Bytes gebildet.</li>
 *   <li>Der Report wird in {@code generated_report} persistiert
 *       (Immutable).</li>
 * </ol>
 *
 * <p>Audit: die Persistierung bildet den verbindlichen Audit-Eintrag.
 * Der SHA-256-Hash ist Teil des Datensatzes und ermoeglicht
 * Manipulationsnachweis.
 */
@Service
public class ReportGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(ReportGeneratorService.class);

    private final HardeningReportDataLoader loader;
    private final HardeningReportTemplateRenderer templateRenderer;
    private final HardeningReportPdfRenderer pdfRenderer;
    private final GeneratedReportRepository repository;
    private final Clock clock;

    public ReportGeneratorService(
            HardeningReportDataLoader loader,
            HardeningReportTemplateRenderer templateRenderer,
            HardeningReportPdfRenderer pdfRenderer,
            GeneratedReportRepository repository,
            Clock clock) {
        this.loader = loader;
        this.templateRenderer = templateRenderer;
        this.pdfRenderer = pdfRenderer;
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public GeneratedReportView generateHardeningReport(HardeningReportInput input) {
        validate(input);
        HardeningReportData data = loader.load(input);
        String html = templateRenderer.render(data);
        Instant creationTime = clock.instant();
        String documentId = buildDocumentId(input, data);
        byte[] pdfBytes = pdfRenderer.render(html, creationTime, documentId);
        String sha = sha256(pdfBytes);

        GeneratedReport report = GeneratedReport.builder()
                .productVersionId(input.productVersionId())
                .environmentId(input.environmentId())
                .reportType("HARDENING")
                .title(buildTitle(data))
                .gesamteinstufung(input.gesamteinstufung())
                .freigeberKommentar(input.freigeberKommentar())
                .erzeugtVon(input.erzeugtVon())
                .erzeugtAm(creationTime)
                .stichtag(input.stichtag())
                .sha256(sha)
                .pdfBytes(pdfBytes)
                .build();
        GeneratedReport persistiert = repository.save(report);
        log.info(
                "Hardening-Report erzeugt: id={}, sha256={}, bytes={}, produkt={}, umgebung={}",
                persistiert.getId(), sha, pdfBytes.length,
                data.kopf().produkt(), data.kopf().umgebung());
        return GeneratedReportView.from(persistiert);
    }

    @Transactional(readOnly = true)
    public GeneratedReportView findById(UUID reportId) {
        GeneratedReport r = repository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));
        return GeneratedReportView.from(r);
    }

    private static void validate(HardeningReportInput input) {
        if (input.productVersionId() == null) {
            throw new IllegalArgumentException("productVersionId darf nicht null sein.");
        }
        if (input.environmentId() == null) {
            throw new IllegalArgumentException("environmentId darf nicht null sein.");
        }
        if (input.gesamteinstufung() == null) {
            throw new IllegalArgumentException("gesamteinstufung darf nicht null sein.");
        }
        if (input.erzeugtVon() == null || input.erzeugtVon().isBlank()) {
            throw new IllegalArgumentException("erzeugtVon darf nicht leer sein.");
        }
        if (input.stichtag() == null) {
            throw new IllegalArgumentException("stichtag darf nicht null sein.");
        }
    }

    private static String buildTitle(HardeningReportData data) {
        return "Hardening-Report %s %s (%s)".formatted(
                data.kopf().produkt(),
                data.kopf().produktVersion(),
                data.kopf().umgebung());
    }

    /**
     * Stabiler Dokument-Identifier, der in das PDF-ID-Array geschrieben
     * wird. Aus (produktVersionId, environmentId, gesamteinstufung,
     * stichtag) abgeleitet. Erlaubt es dem Determinismus-Test, zwei
     * identische Inputs byte-gleich rendern zu lassen; gleichzeitig
     * unterscheidet sich das ID zwischen zwei unterschiedlichen
     * Reports.
     */
    static String buildDocumentId(HardeningReportInput input, HardeningReportData data) {
        String quelle = String.join("|",
                input.productVersionId().toString(),
                input.environmentId().toString(),
                input.gesamteinstufung().name(),
                Long.toString(input.stichtag().toEpochMilli()),
                data.kopf().produkt(),
                data.kopf().produktVersion());
        return sha256(quelle.getBytes(StandardCharsets.UTF_8));
    }

    static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 nicht verfuegbar", ex);
        }
    }
}
