package com.ahs.cvm.ai.executive;

import com.ahs.cvm.application.report.HardeningReportData;
import com.ahs.cvm.application.report.HardeningReportDataLoader;
import com.ahs.cvm.application.report.HardeningReportInput;
import com.ahs.cvm.application.report.HardeningReportPdfRenderer;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.llm.AiCallAuditService;
import com.ahs.cvm.llm.LlmClient;
import com.ahs.cvm.llm.LlmClient.LlmRequest;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClient.Message;
import com.ahs.cvm.llm.LlmClientSelector;
import com.ahs.cvm.llm.prompt.PromptTemplate;
import com.ahs.cvm.llm.prompt.PromptTemplateLoader;
import com.fasterxml.jackson.databind.JsonNode;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Erzeugt den Executive-/Board-/Audit-Report (Iteration 19, CVM-50).
 *
 * <p>Der Service nutzt den {@link HardeningReportDataLoader} aus
 * Iteration 10 als Kennzahlenquelle und
 * {@link HardeningReportPdfRenderer} fuer die deterministische
 * PDF-Erzeugung. Die Bullet-Points werden ueber einen LLM-Call
 * (use-case {@code EXECUTIVE_SUMMARY}) erzeugt und durch den
 * {@link ExecutiveSummary.Validator} auf die harten Limits gebracht.
 */
@Service
public class ExecutiveReportService {

    private static final Logger log = LoggerFactory.getLogger(ExecutiveReportService.class);
    private static final String USE_CASE = "EXECUTIVE_SUMMARY";
    private static final String TEMPLATE_ID = "executive-summary";
    private static final DateTimeFormatter STICHTAG =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final HardeningReportDataLoader dataLoader;
    private final HardeningReportPdfRenderer pdfRenderer;
    private final AiCallAuditService auditService;
    private final LlmClientSelector clientSelector;
    private final PromptTemplateLoader promptLoader;
    private final TemplateEngine templateEngine;
    private final Clock clock;

    public ExecutiveReportService(
            HardeningReportDataLoader dataLoader,
            HardeningReportPdfRenderer pdfRenderer,
            AiCallAuditService auditService,
            LlmClientSelector clientSelector,
            PromptTemplateLoader promptLoader,
            Clock clock) {
        this.dataLoader = dataLoader;
        this.pdfRenderer = pdfRenderer;
        this.auditService = auditService;
        this.clientSelector = clientSelector;
        this.promptLoader = promptLoader;
        this.clock = clock;
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("cvm/reports/executive/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    public ExecutiveReportResult generate(
            UUID productVersionId, UUID environmentId,
            Audience audience, String triggeredBy) {
        if (audience == null) {
            audience = Audience.BOARD;
        }
        HardeningReportInput input = new HardeningReportInput(
                productVersionId, environmentId,
                AhsSeverity.MEDIUM, "Executive-Generierung",
                triggeredBy, Instant.now(clock));
        HardeningReportData data = dataLoader.load(input);
        Counts counts = counts(data);
        ExecutiveSummary summary = erzeugeSummary(data, counts, triggeredBy);

        Context ctx = new Context();
        ctx.setVariable("headline", summary.headline());
        ctx.setVariable("ampel", summary.ampel());
        ctx.setVariable("bullets", summary.bullets());
        ctx.setVariable("produkt", data.kopf().produkt());
        ctx.setVariable("produktVersion", data.kopf().produktVersion());
        ctx.setVariable("umgebung", data.kopf().umgebung());
        ctx.setVariable("stichtag", STICHTAG.format(Instant.now(clock)));
        ctx.setVariable("critical", counts.critical);
        ctx.setVariable("high", counts.high);
        ctx.setVariable("medium", counts.medium);
        ctx.setVariable("kev", counts.kev);
        ctx.setVariable("neu", 0);
        ctx.setVariable("entfallen", 0);
        ctx.setVariable("shifts", 0);
        ctx.setVariable("offenePunkte", data.offenePunkte().stream()
                .limit(5)
                .map(o -> o.cveKey() + ": " + o.severity())
                .toList());

        String templateName = audience == Audience.BOARD ? "board" : "audit";
        String html = templateEngine.process(templateName, ctx);
        byte[] pdf = pdfRenderer.render(html, Instant.now(clock), docId(input, audience));
        return new ExecutiveReportResult(summary, pdf);
    }

    private ExecutiveSummary erzeugeSummary(HardeningReportData data, Counts counts,
            String triggeredBy) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("produkt", data.kopf().produkt());
        vars.put("produktVersion", data.kopf().produktVersion());
        vars.put("umgebung", data.kopf().umgebung());
        vars.put("stichtag", data.kopf().stichtag());
        vars.put("critical", counts.critical);
        vars.put("high", counts.high);
        vars.put("medium", counts.medium);
        vars.put("kev", counts.kev);
        vars.put("neu", 0);
        vars.put("entfallen", 0);
        vars.put("shifts", 0);
        vars.put("offenePunkte", data.offenePunkte().stream()
                .limit(5)
                .map(o -> "- " + o.cveKey() + " " + o.severity())
                .toList());

        PromptTemplate template = promptLoader.load(TEMPLATE_ID);
        LlmClient client = clientSelector.select(null, USE_CASE);
        LlmRequest req = new LlmRequest(
                USE_CASE, template.id(), template.version(),
                template.renderSystem(Map.of()),
                List.of(new Message(Message.Role.USER, template.renderUser(vars))),
                null, 0.2, 1024, null, triggeredBy, null, Map.of());
        try {
            LlmResponse res = auditService.execute(client, req);
            return new ExecutiveSummary.Validator().enforce(
                    parseSummary(res.structuredOutput()));
        } catch (RuntimeException ex) {
            log.warn("Executive-Summary LLM-Fallback: {}", ex.getMessage());
            return new ExecutiveSummary.Validator().enforce(new ExecutiveSummary(
                    "Bericht " + data.kopf().produkt(),
                    ampelAus(counts),
                    fallbackBullets(counts, data)));
        }
    }

    static ExecutiveSummary parseSummary(JsonNode out) {
        if (out == null || out.isMissingNode()) {
            return new ExecutiveSummary("Kein Summary-Output", "YELLOW", List.of());
        }
        String headline = out.path("headline").asText("");
        String ampel = out.path("ampel").asText("YELLOW");
        List<String> bullets = new ArrayList<>();
        if (out.has("bullets") && out.get("bullets").isArray()) {
            out.get("bullets").forEach(b -> {
                if (b.isTextual()) {
                    bullets.add(b.asText());
                }
            });
        }
        return new ExecutiveSummary(headline, ampel, bullets);
    }

    private String ampelAus(Counts c) {
        if (c.critical > 0 || c.kev > 0) {
            return "RED";
        }
        if (c.high > 0) {
            return "YELLOW";
        }
        return "GREEN";
    }

    private List<String> fallbackBullets(Counts c, HardeningReportData data) {
        List<String> list = new ArrayList<>();
        list.add("Produkt " + data.kopf().produkt() + " " + data.kopf().produktVersion()
                + " in " + data.kopf().umgebung() + ".");
        list.add("Offene CRITICAL: " + c.critical + ", HIGH: " + c.high
                + ", MEDIUM: " + c.medium + ".");
        if (c.kev > 0) {
            list.add("KEV-relevante CVEs offen: " + c.kev + ".");
        }
        return list;
    }

    private static Counts counts(HardeningReportData data) {
        int critical = 0, high = 0, medium = 0, kev = 0;
        for (var z : data.kennzahlen()) {
            critical += z.critical();
            high += z.high();
            medium += z.medium();
        }
        for (var c : data.cveListe()) {
            if ("KEV".equalsIgnoreCase(c.originalSeverity())) {
                kev++;
            }
        }
        return new Counts(critical, high, medium, kev);
    }

    private static String docId(HardeningReportInput input, Audience audience) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(input.productVersionId().toString().getBytes());
            md.update(input.environmentId().toString().getBytes());
            md.update(audience.name().getBytes());
            md.update(Long.toString(input.stichtag().toEpochMilli()).getBytes());
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            return "00".repeat(32);
        }
    }

    record Counts(int critical, int high, int medium, int kev) {}

    public enum Audience { BOARD, AUDIT }

    /** Ergebnis: Summary-Objekt + PDF-Bytes. */
    public record ExecutiveReportResult(ExecutiveSummary summary, byte[] pdf) {}
}
