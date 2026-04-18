package com.ahs.cvm.application.report;

import com.ahs.cvm.domain.enums.AhsSeverity;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Vollstaendiges, templating-freundliches Read-Model fuer den
 * Hardening-Report. Die Felder korrespondieren 1:1 zu Abschnitten im
 * Thymeleaf-Template {@code cvm/reports/hardening-report.html}.
 *
 * <p>Alle Listen sind bereits sortiert und stabil (wichtig fuer den
 * Determinismus-Test).
 */
public record HardeningReportData(
        Kopf kopf,
        AhsSeverity gesamteinstufung,
        String freigeberKommentar,
        List<KennzahlZeile> kennzahlen,
        List<CveZeile> cveListe,
        List<OffenerPunkt> offenePunkte,
        Anhang anhang) {

    /**
     * Kopfdaten. Formatierte Strings, damit das Template keine
     * Formatter-Aufrufe braucht (Determinismus + einfachere Tests).
     */
    public record Kopf(
            String produkt,
            String produktVersion,
            String gitCommit,
            String umgebung,
            String umgebungStage,
            String stichtag,
            String erzeugtVon,
            String profilVersion,
            Instant profilGueltigSeit) {}

    /** Eine Zeile der Kennzahlen-Tabelle ("Plattform/Docker/Java/..."). */
    public record KennzahlZeile(
            String kategorie,
            int critical,
            int high,
            int medium,
            int low,
            int informational) {

        public int gesamt() {
            return critical + high + medium + low + informational;
        }
    }

    /** Eine Zeile der CVE-Liste. */
    public record CveZeile(
            String kategorie,
            String cveKey,
            String cveUrl,
            String originalSeverity,
            AhsSeverity ahsSeverity,
            String geplanteBehebung,
            String hinweise) {}

    /** Eintrag fuer "offene kritische Punkte". */
    public record OffenerPunkt(
            String cveKey,
            AhsSeverity severity,
            String status,
            String hinweis) {}

    /**
     * Anhang: Profil-Snapshot (YAML-Auszug), verwendete Regeln,
     * VEX-Platzhalter.
     */
    public record Anhang(
            String profilYamlAuszug,
            List<VerwendeteRegel> verwendeteRegeln,
            Map<String, String> vexPlatzhalter) {}

    public record VerwendeteRegel(
            String ruleKey,
            String name,
            AhsSeverity proposedSeverity,
            int treffer) {}
}
