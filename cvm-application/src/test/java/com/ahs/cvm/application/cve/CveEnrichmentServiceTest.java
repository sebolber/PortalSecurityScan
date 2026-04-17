package com.ahs.cvm.application.cve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.application.cve.CveEnrichmentPort.FeedEnrichment;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.cve.CveRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CveEnrichmentServiceTest {

    private final CveRepository cveRepository = Mockito.mock(CveRepository.class);

    @Test
    @DisplayName("Service: frische CVE (last_fetched_at < 24h) wird nicht erneut abgerufen")
    void refreshCacheHitVermeidetFeedAufruf() {
        CveEnrichmentPort port = Mockito.mock(CveEnrichmentPort.class);
        given(port.feedName()).willReturn("NVD");
        given(port.isEnabled()).willReturn(true);

        Cve frisch = Cve.builder()
                .cveId("CVE-2017-18640")
                .source("NVD")
                .lastFetchedAt(Instant.now().minus(Duration.ofMinutes(30)))
                .build();
        given(cveRepository.findByCveId("CVE-2017-18640")).willReturn(Optional.of(frisch));
        given(cveRepository.save(any(Cve.class))).willAnswer(inv -> inv.getArgument(0));

        CveEnrichmentService service = new CveEnrichmentService(
                cveRepository, List.of(port), Duration.ofHours(24));

        service.enrich("CVE-2017-18640");

        verify(port, never()).fetch(any());
    }

    @Test
    @DisplayName("Service: veraltete CVE fuehrt zu Feed-Abruf und Uebernahme der Werte")
    void staleCacheTriggertAbruf() {
        CveEnrichmentPort port = Mockito.mock(CveEnrichmentPort.class);
        given(port.feedName()).willReturn("NVD");
        given(port.isEnabled()).willReturn(true);
        given(port.fetch("CVE-2017-18640"))
                .willReturn(Optional.of(new FeedEnrichment(
                        "CVE-2017-18640",
                        "NVD",
                        "SnakeYAML billion laughs",
                        new BigDecimal("7.5"),
                        "CVSS:3.1/AV:N",
                        null, null, null, null,
                        List.of("CWE-776"),
                        null)));

        Cve stale = Cve.builder()
                .cveId("CVE-2017-18640")
                .source("NVD")
                .lastFetchedAt(Instant.now().minus(Duration.ofDays(3)))
                .build();
        given(cveRepository.findByCveId("CVE-2017-18640")).willReturn(Optional.of(stale));
        given(cveRepository.save(any(Cve.class))).willAnswer(inv -> inv.getArgument(0));

        CveEnrichmentService service = new CveEnrichmentService(
                cveRepository, List.of(port), Duration.ofHours(24));

        Cve ergebnis = service.enrich("CVE-2017-18640").orElseThrow();

        assertThat(ergebnis.getCvssBaseScore()).isEqualByComparingTo(new BigDecimal("7.5"));
        assertThat(ergebnis.getCwes()).containsExactly("CWE-776");
        assertThat(ergebnis.getSummary()).contains("billion laughs");
    }
}
