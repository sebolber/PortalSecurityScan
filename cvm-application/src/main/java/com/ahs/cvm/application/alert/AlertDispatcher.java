package com.ahs.cvm.application.alert;

import com.ahs.cvm.persistence.alert.AlertDispatch;
import com.ahs.cvm.persistence.alert.AlertDispatchRepository;
import com.ahs.cvm.persistence.alert.AlertRule;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Rendert das Mail-Template, ruft den {@link MailSenderPort} und
 * schreibt einen {@link AlertDispatch}-Eintrag.
 *
 * <p>Im Dry-Run-Modus wird der Mail-Sender ueberhaupt nicht
 * angesprochen; der Audit-Eintrag enthaelt {@code dryRun=true}.
 */
@Service
public class AlertDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AlertDispatcher.class);
    private static final int BODY_AUSZUG_LAENGE = 240;

    private final AlertTemplateRenderer renderer;
    private final MailSenderPort mailSender;
    private final AlertDispatchRepository dispatchRepository;
    private final AlertConfig config;
    private final Clock clock;

    public AlertDispatcher(
            AlertTemplateRenderer renderer,
            MailSenderPort mailSender,
            AlertDispatchRepository dispatchRepository,
            AlertConfig config,
            Clock clock) {
        this.renderer = renderer;
        this.mailSender = mailSender;
        this.dispatchRepository = dispatchRepository;
        this.config = config;
        this.clock = clock;
    }

    public void dispatch(AlertRule rule, AlertContext context) {
        Map<String, Object> daten = sammleDaten(rule, context);
        String html = renderer.render(rule.getTemplateName(), daten);
        String subject = buildSubject(rule, context);
        Instant jetzt = Instant.now(clock);
        if (config.dryRun()) {
            log.info("Alert dry-run: rule={}, key={}, subject='{}'",
                    rule.getName(), context.triggerKey(), subject);
            persistiere(rule, context, subject, html, jetzt, true, null);
            return;
        }
        try {
            mailSender.send(rule.getRecipients(), subject, html);
            persistiere(rule, context, subject, html, jetzt, false, null);
        } catch (RuntimeException ex) {
            persistiere(rule, context, subject, html, jetzt, false, ex.getMessage());
            throw new MailDispatchException(
                    "Alert-Versand fehlgeschlagen fuer Regel '" + rule.getName() + "'", ex);
        }
    }

    private Map<String, Object> sammleDaten(AlertRule rule, AlertContext context) {
        Map<String, Object> daten = new LinkedHashMap<>();
        daten.put("subjectPrefix", rule.getSubjectPrefix());
        daten.put("severity", context.severity() == null ? "" : context.severity().name());
        daten.put("triggerArt", context.triggerArt().name());
        daten.put("triggerKey", context.triggerKey());
        daten.put("summary", context.summary() == null ? "" : context.summary());
        daten.put("cveKey", attribute(context, "cveKey", ""));
        daten.put("cveUrl", attribute(context, "cveUrl", ""));
        daten.put("queueUrl", attribute(context, "queueUrl", ""));
        daten.put("produktVersion", attribute(context, "produktVersion", ""));
        daten.put("umgebung", attribute(context, "umgebung", ""));
        daten.put("quelle", attribute(context, "quelle", ""));
        daten.put("alterMinuten", attribute(context, "alterMinuten", "0"));
        daten.putAll(extraAttributes(context));
        return daten;
    }

    private Object attribute(AlertContext context, String key, Object fallback) {
        Object wert = context.attributes().get(key);
        return wert == null ? fallback : wert;
    }

    private Map<String, Object> extraAttributes(AlertContext context) {
        Map<String, Object> ohneKollision = new HashMap<>(context.attributes());
        ohneKollision.keySet().removeAll(java.util.List.of(
                "cveKey", "cveUrl", "queueUrl", "produktVersion",
                "umgebung", "quelle", "alterMinuten"));
        return ohneKollision;
    }

    private String buildSubject(AlertRule rule, AlertContext context) {
        String prefix = rule.getSubjectPrefix() == null ? "[CVM]" : rule.getSubjectPrefix();
        String kennzeichen = context.attributes().getOrDefault("cveKey",
                context.triggerKey()).toString();
        return prefix + " " + rule.getName() + " - " + kennzeichen;
    }

    private void persistiere(
            AlertRule rule,
            AlertContext context,
            String subject,
            String html,
            Instant jetzt,
            boolean dryRun,
            String fehler) {
        AlertDispatch dispatch = AlertDispatch.builder()
                .ruleId(rule.getId())
                .triggerKey(context.triggerKey())
                .dispatchedAt(jetzt)
                .recipients(List.copyOf(rule.getRecipients()))
                .subject(subject)
                .bodyExcerpt(kuerze(html))
                .dryRun(dryRun)
                .error(fehler)
                .build();
        dispatchRepository.save(dispatch);
    }

    private String kuerze(String html) {
        if (html == null) {
            return null;
        }
        if (html.length() <= BODY_AUSZUG_LAENGE) {
            return html;
        }
        return html.substring(0, BODY_AUSZUG_LAENGE);
    }
}
