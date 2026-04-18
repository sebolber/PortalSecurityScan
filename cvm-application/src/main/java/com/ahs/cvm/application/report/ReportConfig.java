package com.ahs.cvm.application.report;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Konfiguration fuer das Report-Subsystem (Iteration 10, CVM-19).
 *
 * <ul>
 *   <li>Eigene {@link TemplateEngine}-Instanz auf
 *       {@code classpath:cvm/reports/} begrenzt. Damit bleibt das
 *       Report-Rendering unabhaengig von einer globalen Thymeleaf-
 *       AutoConfig im App-Modul.</li>
 *   <li>Fallback-{@link Clock}-Bean, sollte noch keine existieren.</li>
 * </ul>
 */
@Configuration
public class ReportConfig {

    @Bean
    public TemplateEngine reportTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("cvm/reports/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock reportClock() {
        return Clock.systemUTC();
    }
}
