package com.ahs.cvm.application.report;

import com.ahs.cvm.domain.enums.AhsSeverity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Synthetische Test-Daten fuer den Hardening-Report. Bewusst fix, damit
 * Tests stabil bleiben (Sortierung, Determinismus).
 */
final class HardeningReportFixtures {

    static final UUID PRODUCT_VERSION_ID =
            UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    static final UUID ENVIRONMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    static final Instant STICHTAG = Instant.parse("2026-04-18T00:00:00Z");

    private HardeningReportFixtures() {}

    static HardeningReportInput input() {
        return new HardeningReportInput(
                PRODUCT_VERSION_ID,
                ENVIRONMENT_ID,
                AhsSeverity.MEDIUM,
                "Gesamteinstufung bestaetigt durch Freigeber.",
                "a.admin@ahs.test",
                STICHTAG);
    }

    static HardeningReportData data() {
        HardeningReportData.Kopf kopf = new HardeningReportData.Kopf(
                "PortalCore-Test",
                "1.14.2-test",
                "a3f9beef",
                "REF-TEST",
                "REFERENCE",
                "2026-04-18",
                "a.admin@ahs.test",
                "v3",
                Instant.parse("2026-03-01T00:00:00Z"));

        List<HardeningReportData.KennzahlZeile> kennzahlen = List.of(
                new HardeningReportData.KennzahlZeile("Plattform", 1, 0, 2, 0, 0),
                new HardeningReportData.KennzahlZeile("Docker", 0, 1, 0, 0, 0),
                new HardeningReportData.KennzahlZeile("Java", 0, 0, 1, 3, 0),
                new HardeningReportData.KennzahlZeile("NodeJS", 0, 0, 0, 0, 1),
                new HardeningReportData.KennzahlZeile("Python", 0, 0, 0, 0, 0));

        List<HardeningReportData.CveZeile> cveListe = List.of(
                new HardeningReportData.CveZeile(
                        "Java", "CVE-2017-18640",
                        "https://nvd.nist.gov/vuln/detail/CVE-2017-18640",
                        "7.5", AhsSeverity.MEDIUM,
                        "Update auf 1.15.0", "Kein Nutzer-Input im Pfad."),
                new HardeningReportData.CveZeile(
                        "Plattform", "CVE-2025-48924",
                        "https://nvd.nist.gov/vuln/detail/CVE-2025-48924",
                        "9.8", AhsSeverity.CRITICAL,
                        "Patch Maerz 2026", "Regel PC-2025-01 getroffen."));

        List<HardeningReportData.OffenerPunkt> offenePunkte = List.of(
                new HardeningReportData.OffenerPunkt(
                        "CVE-2026-22610", AhsSeverity.HIGH, "PROPOSED",
                        "Wartet auf manuelle Bewertung durch t.tester@ahs.test."));

        HardeningReportData.Anhang anhang = new HardeningReportData.Anhang(
                "schemaVersion: 1\nproduct: PortalCore-Test\n",
                List.of(new HardeningReportData.VerwendeteRegel(
                        "PC-2025-01", "Plattform-Kritisch",
                        AhsSeverity.CRITICAL, 1)),
                Map.of("vex", "Platzhalter - Ausgabe folgt in Iteration 20."));

        return new HardeningReportData(
                kopf,
                AhsSeverity.MEDIUM,
                "Gesamteinstufung bestaetigt durch Freigeber.",
                kennzahlen,
                cveListe,
                offenePunkte,
                anhang);
    }
}
