package com.ahs.cvm.ai.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScanDeltaCalculatorTest {

    private final FindingRepository repo = mock(FindingRepository.class);
    private final ScanDeltaCalculator calc = new ScanDeltaCalculator(repo);

    private Finding finding(String cveKey, double cvss, boolean kev) {
        Cve cve = Cve.builder()
                .id(UUID.randomUUID())
                .cveId(cveKey)
                .cvssBaseScore(BigDecimal.valueOf(cvss))
                .kevListed(kev)
                .build();
        return Finding.builder().cve(cve).build();
    }

    @Test
    @DisplayName("Delta: Initial-Run liefert alle CVEs als 'neu', kein Diff")
    void initial() {
        UUID scan = UUID.randomUUID();
        given(repo.findByScanId(scan)).willReturn(List.of(
                finding("CVE-A", 5.0, false), finding("CVE-B", 7.5, false)));

        ScanDelta delta = calc.calculate(scan, null);

        assertThat(delta.neueCves()).containsExactly("CVE-A", "CVE-B");
        assertThat(delta.entfalleneCves()).isEmpty();
        assertThat(delta.severityShifts()).isEmpty();
        assertThat(delta.kevAenderungen()).isEmpty();
    }

    @Test
    @DisplayName("Delta: erkennt neue, entfallene und Severity-Shifts")
    void diffMitShift() {
        UUID curr = UUID.randomUUID();
        UUID prev = UUID.randomUUID();
        given(repo.findByScanId(curr)).willReturn(List.of(
                finding("CVE-A", 9.5, false),    // shift HIGH->CRITICAL
                finding("CVE-NEU", 5.0, false))); // neu
        given(repo.findByScanId(prev)).willReturn(List.of(
                finding("CVE-A", 7.5, false),    // war HIGH
                finding("CVE-WEG", 8.0, false))); // entfallen

        ScanDelta delta = calc.calculate(curr, prev);

        assertThat(delta.neueCves()).containsExactly("CVE-NEU");
        assertThat(delta.entfalleneCves()).containsExactly("CVE-WEG");
        assertThat(delta.severityShifts()).hasSize(1);
        assertThat(delta.severityShifts().get(0).cveKey()).isEqualTo("CVE-A");
        assertThat(delta.severityShifts().get(0).von()).isEqualTo("HIGH");
        assertThat(delta.severityShifts().get(0).nach()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("Delta: KEV-Aenderung wird gemeldet")
    void kevAenderung() {
        UUID curr = UUID.randomUUID();
        UUID prev = UUID.randomUUID();
        given(repo.findByScanId(curr)).willReturn(List.of(
                finding("CVE-A", 7.5, true)));
        given(repo.findByScanId(prev)).willReturn(List.of(
                finding("CVE-A", 7.5, false)));

        ScanDelta delta = calc.calculate(curr, prev);

        assertThat(delta.kevAenderungen()).containsExactly("CVE-A");
        assertThat(delta.severityShifts()).isEmpty();
    }

    @Test
    @DisplayName("Delta: identische Scans liefern leeren Diff")
    void identisch() {
        UUID curr = UUID.randomUUID();
        UUID prev = UUID.randomUUID();
        given(repo.findByScanId(any())).willReturn(List.of(
                finding("CVE-A", 7.5, false)));

        ScanDelta delta = calc.calculate(curr, prev);

        assertThat(delta.totalDelta()).isZero();
    }
}
