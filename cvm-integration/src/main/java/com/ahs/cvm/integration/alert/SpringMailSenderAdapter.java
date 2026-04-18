package com.ahs.cvm.integration.alert;

import com.ahs.cvm.application.alert.AlertConfig;
import com.ahs.cvm.application.alert.AlertTemplateRenderer;
import com.ahs.cvm.application.alert.MailDispatchException;
import com.ahs.cvm.application.alert.MailSenderPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Produktiver Mail-Adapter via {@link JavaMailSender}. Spring Boot's
 * Mail-AutoConfig liefert {@code JavaMailSender} mit, sobald
 * {@code spring-boot-starter-mail} im Classpath ist (im Modul
 * {@code cvm-integration} der Fall). Damit ist dieser Adapter immer
 * aktiv und verdraengt den Noop-Fallback aus
 * {@link com.ahs.cvm.application.alert.AlertConfig}.
 */
@Component
public class SpringMailSenderAdapter implements MailSenderPort {

    private final JavaMailSender mailSender;
    private final AlertTemplateRenderer renderer;
    private final AlertConfig config;

    public SpringMailSenderAdapter(
            JavaMailSender mailSender,
            AlertTemplateRenderer renderer,
            AlertConfig config) {
        this.mailSender = mailSender;
        this.renderer = renderer;
        this.config = config;
    }

    @Override
    public void send(List<String> recipients, String subject, String html) {
        if (recipients == null || recipients.isEmpty()) {
            throw new MailDispatchException("Keine Empfaenger konfiguriert", null);
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(config.fromAddress());
            helper.setTo(recipients.toArray(String[]::new));
            helper.setSubject(subject);
            helper.setText(renderer.toPlainText(html), html);
            mailSender.send(message);
        } catch (MessagingException | MailException ex) {
            throw new MailDispatchException("SMTP-Versand fehlgeschlagen", ex);
        }
    }
}
