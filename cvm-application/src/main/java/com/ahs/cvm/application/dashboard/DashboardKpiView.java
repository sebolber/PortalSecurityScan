package com.ahs.cvm.application.dashboard;

import com.ahs.cvm.domain.enums.AhsSeverity;
import java.util.Map;

/**
 * Iteration 100 (CVM-342): Dashboard-KPIs. Ersetzt die frueheren
 * Dummy-Werte im Frontend durch echte Zahlen aus dem Assessment-
 * Store.
 *
 * @param offeneCves   Anzahl offener Assessments
 *                     (Status PROPOSED/NEEDS_REVIEW/NEEDS_VERIFICATION,
 *                     noch nicht superseded).
 * @param severityVerteilung  Zaehler pro Severity (nur offene).
 * @param aeltesteKritisch    CVE-Key + Tage Alter des aeltesten
 *                            offenen CRITICAL-Assessments; {@code null}
 *                            wenn kein offenes CRITICAL existiert.
 * @param weiterbetriebOk     {@code false}, sobald ein offenes
 *                            CRITICAL-Assessment aelter als
 *                            {@code weiterbetriebSchwelleTage} ist
 *                            oder mehr als
 *                            {@code weiterbetriebSchwelleCount}
 *                            offene HIGH/CRITICAL existieren.
 */
public record DashboardKpiView(
        long offeneCves,
        Map<AhsSeverity, Long> severityVerteilung,
        AeltesteKritisch aeltesteKritisch,
        boolean weiterbetriebOk) {

    public record AeltesteKritisch(String cveKey, long tage) {}
}
