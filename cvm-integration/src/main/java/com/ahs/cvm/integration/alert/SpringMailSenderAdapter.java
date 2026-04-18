package com.ahs.cvm.integration.alert;

import com.ahs.cvm.application.alert.AlertConfig;
import com.ahs.cvm.application.alert.AlertTemplateRenderer;
import com.ahs.cvm.application.alert.MailDispatchException;
import com.ahs.cvm.application.alert.MailSenderPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Produktiver Mail-Adapter via {@link JavaMailSender}. Wird nur
 * aktiviert, wenn ein {@code JavaMailSender}-Bean (Spring Mail
 * AutoConfig) vorhanden ist.
 */
@Component
@ConditionalOnBean(JavaMailSender.class)
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
