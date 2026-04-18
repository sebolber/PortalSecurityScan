package com.ahs.cvm.ai.summary;

import java.util.List;

/**
 * Strukturierter Diff zwischen zwei Scans (Iteration 14, CVM-33).
 * Wird an den LLM-Prompt uebergeben - kein rohes SBOM.
 *
 * @param neueCves CVE-Keys, die im aktuellen Scan neu hinzugekommen sind.
 * @param entfalleneCves CVE-Keys, die im Vorgaenger waren, im aktuellen
 *     Scan aber nicht mehr.
 * @param severityShifts CVE-Keys mit veraenderter Severity-Einschaetzung.
 * @param kevAenderungen CVE-Keys, deren KEV-Status sich geaendert hat.
 */
public record ScanDelta(
        List<String> neueCves,
        List<String> entfalleneCves,
        List<SeverityShift> severityShifts,
        List<String> kevAenderungen) {

    public ScanDelta {
        neueCves = neueCves == null ? List.of() : List.copyOf(neueCves);
        entfalleneCves = entfalleneCves == null
                ? List.of() : List.copyOf(entfalleneCves);
        severityShifts = severityShifts == null
                ? List.of() : List.copyOf(severityShifts);
        kevAenderungen = kevAenderungen == null
                ? List.of() : List.copyOf(kevAenderungen);
    }

    public int totalDelta() {
        return neueCves.size() + entfalleneCves.size()
                + severityShifts.size() + kevAenderungen.size();
    }

    public record SeverityShift(String cveKey, String von, String nach) {}
}
