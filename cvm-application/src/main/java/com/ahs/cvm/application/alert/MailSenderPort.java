package com.ahs.cvm.application.alert;

import java.util.List;

/**
 * Outgoing-Port fuer den Mail-Versand. Ein produktiver Adapter sitzt in
 * {@code cvm-integration/alert/SpringMailSenderAdapter}; Tests stellen
 * eine eigene Implementierung bereit (z.&nbsp;B. eine
 * {@code RecordingMailSender}-Klasse).
 */
public interface MailSenderPort {

    /**
     * Versendet eine HTML-Mail. Es wird empfohlen, einen Plain-Text-
     * Fallback aus dem HTML zu erzeugen; das uebernimmt der Adapter.
     *
     * @param recipients Empfaenger-Adressen.
     * @param subject Subject inkl. Praefix.
     * @param html HTML-Body.
     * @throws MailDispatchException bei Versand- oder Konfig-Fehlern.
     */
    void send(List<String> recipients, String subject, String html);
}
