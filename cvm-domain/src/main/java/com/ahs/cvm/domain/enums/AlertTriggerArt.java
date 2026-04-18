package com.ahs.cvm.domain.enums;

/**
 * Wann wird ein Alert ueberhaupt geprueft? Fachlich aus Konzept v0.2
 * Abschnitt 9 (siehe auch Iterations-Prompt 09-SMTP-Alerts.md).
 *
 * <ul>
 *   <li>{@link #FINDING_NEU} &mdash; ein neues Finding ist eingetroffen
 *       (z.&nbsp;B. KEV oder CRITICAL bei Erstkontakt).</li>
 *   <li>{@link #ASSESSMENT_PROPOSED} &mdash; ein neuer Bewertungs-
 *       Vorschlag wurde angelegt, aber noch nicht freigegeben.</li>
 *   <li>{@link #ASSESSMENT_APPROVED} &mdash; ein Assessment wurde
 *       freigegeben (z.&nbsp;B. Information ans Plattform-Team).</li>
 *   <li>{@link #ESKALATION_T1} &mdash; ein offener kritischer Vorschlag
 *       liegt laenger als T1 (Default 2&nbsp;h) ohne Bewertung.</li>
 *   <li>{@link #ESKALATION_T2} &mdash; T2-Schwelle (Default 6&nbsp;h)
 *       ueberschritten. Setzt zusaetzlich Banner-Flag im Frontend.</li>
 *   <li>{@link #KEV_HIT} &mdash; KEV-Listing ist bei einem bekannten
 *       Finding eingetroffen.</li>
 * </ul>
 */
public enum AlertTriggerArt {
    FINDING_NEU,
    ASSESSMENT_PROPOSED,
    ASSESSMENT_APPROVED,
    ESKALATION_T1,
    ESKALATION_T2,
    KEV_HIT,
    KI_ANOMALIE
}
