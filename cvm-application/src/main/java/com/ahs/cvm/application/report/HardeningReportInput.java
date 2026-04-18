package com.ahs.cvm.application.report;

import com.ahs.cvm.domain.enums.AhsSeverity;
import java.time.Instant;
import java.util.UUID;

/**
 * Eingabe fuer {@link ReportGeneratorService#generateHardeningReport(HardeningReportInput)}.
 *
 * @param productVersionId Produkt-Version, fuer die der Report gezogen wird.
 * @param environmentId Umgebung, fuer die der Report gezogen wird.
 * @param gesamteinstufung Vom Freigeber bestaetigte oder angepasste
 *     Gesamteinstufung (Konzept 10.1: vorausgewaehlte Ampel).
 * @param freigeberKommentar Optionaler Kommentar des Freigebers.
 * @param erzeugtVon Login des Freigebers. Pflicht.
 * @param stichtag Stichtag, zu dem der Snapshot gebildet wird. Wird im
 *     Report-Kopf und im Dateinamen gefuehrt. Typisch "jetzt"; Tests
 *     koennen einen fixen Zeitpunkt uebergeben.
 */
public record HardeningReportInput(
        UUID productVersionId,
        UUID environmentId,
        AhsSeverity gesamteinstufung,
        String freigeberKommentar,
        String erzeugtVon,
        Instant stichtag) {}
