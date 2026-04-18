package com.ahs.cvm.application.report;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Rendert das Thymeleaf-Template
 * {@code cvm/reports/hardening-report.html} zu einem HTML-String.
 * Die Variablen kommen 1:1 aus {@link HardeningReportData}.
 *
 * <p>Der Renderer verwendet bewusst einen dedizierten
 * {@link TemplateEngine}, der im {@link ReportConfig} konfiguriert ist
 * (Classpath-Resolver, HTML-Mode, UTF-8). Das vermeidet Kopplung an
 * eine globale Spring-Web-AutoConfig, die im Application-Modul nicht
 * vorhanden ist.
 */
@Component
public class HardeningReportTemplateRenderer {

    private static final String TEMPLATE_NAME = "hardening-report";

    private final TemplateEngine templateEngine;

    public HardeningReportTemplateRenderer(TemplateEngine reportTemplateEngine) {
        this.templateEngine = reportTemplateEngine;
    }

    public String render(HardeningReportData data) {
        Context ctx = new Context();
        ctx.setVariable("kopf", data.kopf());
        ctx.setVariable("gesamteinstufung", data.gesamteinstufung());
        ctx.setVariable("freigeberKommentar", data.freigeberKommentar());
        ctx.setVariable("kennzahlen", data.kennzahlen());
        ctx.setVariable("cveListe", data.cveListe());
        ctx.setVariable("offenePunkte", data.offenePunkte());
        ctx.setVariable("anhang", data.anhang());
        return templateEngine.process(TEMPLATE_NAME, ctx);
    }
}
