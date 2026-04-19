package com.ahs.cvm.application.alert;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguration fuer das Alert-Subsystem.
 *
 * <ul>
 *   <li>{@code cvm.alerts.mode} &mdash; {@code dry-run} (Default) oder
 *       {@code real}. Im Dry-Run wird kein Mail-Sender aufgerufen, der
 *       Audit-Eintrag aber trotzdem geschrieben.</li>
 *   <li>{@code cvm.alerts.eskalation.t1-minutes} &mdash; T1-Schwelle
 *       (Default 120 = 2&nbsp;h).</li>
 *   <li>{@code cvm.alerts.eskalation.t2-minutes} &mdash; T2-Schwelle
 *       (Default 360 = 6&nbsp;h).</li>
 *   <li>{@code cvm.alerts.from} &mdash; From-Adresse (Default
 *       {@code cvm-alerts@ahs.local}).</li>
 * </ul>
 */
@Configuration
public class AlertConfig {

    private final String mode;
    private final int t1Minutes;
    private final int t2Minutes;
    private final Duration t1;
    private final Duration t2;
    private final String fromAddress;
    private SystemParameterResolver resolver;

    public AlertConfig(
            @Value("${cvm.alerts.mode:dry-run}") String mode,
            @Value("${cvm.alerts.eskalation.t1-minutes:120}") int t1Minutes,
            @Value("${cvm.alerts.eskalation.t2-minutes:360}") int t2Minutes,
            @Value("${cvm.alerts.from:cvm-alerts@ahs.local}") String fromAddress) {
        this.mode = mode == null ? "dry-run" : mode.toLowerCase();
        this.t1Minutes = Math.max(1, t1Minutes);
        this.t2Minutes = Math.max(t1Minutes + 1, t2Minutes);
        this.t1 = Duration.ofMinutes(this.t1Minutes);
        this.t2 = Duration.ofMinutes(this.t2Minutes);
        this.fromAddress = fromAddress;
    }

    @Autowired(required = false)
    public void setResolver(SystemParameterResolver resolver) {
        this.resolver = resolver;
    }

    public boolean dryRun() {
        return !"real".equals(mode) && !"live".equals(mode);
    }

    public Duration t1() {
        return t1;
    }

    public Duration t2() {
        return t2;
    }

    public String fromAddress() {
        return fromAddress;
    }

    public boolean dryRunEffective() {
        if (resolver == null) {
            return dryRun();
        }
        String modeValue = resolver.resolveString("cvm.alerts.mode", mode);
        return !"real".equalsIgnoreCase(modeValue) && !"live".equalsIgnoreCase(modeValue);
    }

    public Duration t1Effective() {
        if (resolver == null) {
            return t1;
        }
        int v = resolver.resolveInt("cvm.alerts.eskalation.t1-minutes", t1Minutes);
        return Duration.ofMinutes(Math.max(1, v));
    }

    public Duration t2Effective() {
        if (resolver == null) {
            return t2;
        }
        int current1 = resolver.resolveInt("cvm.alerts.eskalation.t1-minutes", t1Minutes);
        int v = resolver.resolveInt("cvm.alerts.eskalation.t2-minutes", t2Minutes);
        return Duration.ofMinutes(Math.max(Math.max(1, current1) + 1, v));
    }

    public String fromAddressEffective() {
        if (resolver == null) {
            return fromAddress;
        }
        return resolver.resolveString("cvm.alerts.from", fromAddress);
    }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    @org.springframework.context.annotation.Description(
            "Clock fuer Cooldown- und Eskalations-Logik. Tests koennen ueberschreiben.")
    public Clock alertClock() {
        return Clock.systemUTC();
    }

    /**
     * Fallback-Mail-Sender fuer Profile ohne {@code JavaMailSender}-
     * AutoConfig. Loggt nur. Wird durch den Spring-Mail-Adapter
     * automatisch verdraengt, sobald die SMTP-Konfiguration steht.
     */
    @Bean
    @ConditionalOnMissingBean(MailSenderPort.class)
    public MailSenderPort noopMailSender() {
        Logger log = LoggerFactory.getLogger(AlertConfig.class);
        return (List<String> recipients, String subject, String html) ->
                log.warn("Noop-Mail-Sender aktiv: kein SMTP konfiguriert. recipients={}, subject={}",
                        recipients, subject);
    }
}
