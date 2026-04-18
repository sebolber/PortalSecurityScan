package com.ahs.cvm.ai.summary;

import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministische Diff-Berechnung zwischen zwei Scans (Iteration 14).
 * Datenbasis sind die Findings (CVE-Keys + Severity + KEV-Status).
 */
@Component
public class ScanDeltaCalculator {

    private final FindingRepository findingRepository;

    public ScanDeltaCalculator(FindingRepository findingRepository) {
        this.findingRepository = findingRepository;
    }

    @Transactional(readOnly = true)
    public ScanDelta calculate(UUID currentScanId, UUID previousScanId) {
        Map<String, Finding> aktuell = indiziereProCveKey(currentScanId);
        if (previousScanId == null) {
            return new ScanDelta(
                    new ArrayList<>(new TreeSet<>(aktuell.keySet())),
                    List.of(), List.of(), List.of());
        }
        Map<String, Finding> vorher = indiziereProCveKey(previousScanId);

        Set<String> neu = new TreeSet<>(aktuell.keySet());
        neu.removeAll(vorher.keySet());

        Set<String> entfallen = new TreeSet<>(vorher.keySet());
        entfallen.removeAll(aktuell.keySet());

        List<ScanDelta.SeverityShift> shifts = new ArrayList<>();
        List<String> kevAenderungen = new ArrayList<>();
        Set<String> ueberschneidung = new HashSet<>(aktuell.keySet());
        ueberschneidung.retainAll(vorher.keySet());
        for (String cveKey : new TreeSet<>(ueberschneidung)) {
            Cve a = aktuell.get(cveKey).getCve();
            Cve v = vorher.get(cveKey).getCve();
            String aSev = severityLabel(a);
            String vSev = severityLabel(v);
            if (!aSev.equals(vSev)) {
                shifts.add(new ScanDelta.SeverityShift(cveKey, vSev, aSev));
            }
            if (!equalsSafe(a.getKevListed(), v.getKevListed())) {
                kevAenderungen.add(cveKey);
            }
        }
        return new ScanDelta(
                new ArrayList<>(neu),
                new ArrayList<>(entfallen),
                shifts,
                kevAenderungen);
    }

    private Map<String, Finding> indiziereProCveKey(UUID scanId) {
        Map<String, Finding> map = new HashMap<>();
        for (Finding f : findingRepository.findByScanId(scanId)) {
            map.putIfAbsent(f.getCve().getCveId(), f);
        }
        return map;
    }

    static String severityLabel(Cve cve) {
        if (cve.getCvssBaseScore() == null) {
            return "-";
        }
        double s = cve.getCvssBaseScore().doubleValue();
        if (s >= 9.0) return "CRITICAL";
        if (s >= 7.0) return "HIGH";
        if (s >= 4.0) return "MEDIUM";
        if (s > 0.0) return "LOW";
        return "NONE";
    }

    private static boolean equalsSafe(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
