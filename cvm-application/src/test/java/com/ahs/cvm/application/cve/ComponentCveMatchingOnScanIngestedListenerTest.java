package com.ahs.cvm.application.cve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ahs.cvm.application.scan.ScanIngestedEvent;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.cve.CveRepository;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.scan.Component;
import com.ahs.cvm.persistence.scan.ComponentOccurrence;
import com.ahs.cvm.persistence.scan.ComponentOccurrenceRepository;
import com.ahs.cvm.persistence.scan.Scan;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ComponentCveMatchingOnScanIngestedListenerTest {

    private static final UUID SCAN_ID = UUID.randomUUID();

    private ComponentOccurrenceRepository occurrenceRepository;
    private FindingRepository findingRepository;
    private CveRepository cveRepository;
    private ComponentVulnerabilityLookup lookup;
    private ComponentCveMatchingOnScanIngestedListener listener;

    @BeforeEach
    void setUp() {
        occurrenceRepository = mock(ComponentOccurrenceRepository.class);
        findingRepository = mock(FindingRepository.class);
        cveRepository = mock(CveRepository.class);
        lookup = mock(ComponentVulnerabilityLookup.class);
        listener = new ComponentCveMatchingOnScanIngestedListener(
                occurrenceRepository, findingRepository, cveRepository, lookup);
    }

    @Test
    @DisplayName(
        "Reflection-Guard: onScanIngested hat "
            + "@TransactionalEventListener + @Transactional(REQUIRES_NEW)")
    void annotationPropagationWieVonSpringVerlangt() throws Exception {
        java.lang.reflect.Method m = ComponentCveMatchingOnScanIngestedListener
                .class
                .getMethod("onScanIngested", ScanIngestedEvent.class);

        assertThat(m.isAnnotationPresent(
                org.springframework.transaction.event.TransactionalEventListener.class))
                .as("@TransactionalEventListener muss vorhanden sein")
                .isTrue();

        org.springframework.transaction.annotation.Transactional tx = m
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(tx).as("@Transactional muss vorhanden sein").isNotNull();
        // Sonst: Spring lehnt den Context-Start ab mit
        // "must not be annotated with @Transactional unless declared
        //  as REQUIRES_NEW or NOT_SUPPORTED".
        assertThat(tx.propagation())
                .as("Propagation muss REQUIRES_NEW oder NOT_SUPPORTED sein")
                .isIn(
                        org.springframework.transaction.annotation.Propagation.REQUIRES_NEW,
                        org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED);
    }

    @Test
    @DisplayName("Listener skippt alles, wenn OSV-Lookup deaktiviert ist")
    void skipWennDeaktiviert() {
        given(lookup.isEnabled()).willReturn(false);

        listener.onScanIngested(new ScanIngestedEvent(SCAN_ID, UUID.randomUUID(), null, 1, 0, java.time.Instant.now()));

        verify(occurrenceRepository, never()).findByScanId(any());
        verify(findingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Listener legt pro Treffer genau ein Finding an")
    void happyPath() {
        given(lookup.isEnabled()).willReturn(true);

        ComponentOccurrence occ = occurrence(
                "pkg:npm/axios@1.6.7", UUID.randomUUID());
        given(occurrenceRepository.findByScanId(SCAN_ID))
                .willReturn(List.of(occ));
        given(lookup.findCveIdsForPurls(anyList()))
                .willReturn(Map.of(
                        "pkg:npm/axios@1.6.7",
                        List.of("CVE-2024-39338")));
        given(cveRepository.findByCveId("CVE-2024-39338"))
                .willReturn(Optional.empty());
        when(cveRepository.save(any(Cve.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(findingRepository.save(any(Finding.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        given(findingRepository
                .existsByScanIdAndComponentOccurrenceIdAndCveCveId(
                        any(), any(), any()))
                .willReturn(false);

        listener.onScanIngested(new ScanIngestedEvent(SCAN_ID, UUID.randomUUID(), null, 1, 0, java.time.Instant.now()));

        verify(cveRepository).save(any(Cve.class));
        verify(findingRepository).save(any(Finding.class));
    }

    @Test
    @DisplayName("Dedup: existiert das Finding schon, wird nichts gespeichert")
    void dedup() {
        given(lookup.isEnabled()).willReturn(true);
        ComponentOccurrence occ = occurrence(
                "pkg:maven/org.apache.tomcat/tomcat-catalina@9.0.85", UUID.randomUUID());
        given(occurrenceRepository.findByScanId(SCAN_ID))
                .willReturn(List.of(occ));
        given(lookup.findCveIdsForPurls(anyList()))
                .willReturn(Map.of(occ.getComponent().getPurl(),
                        List.of("CVE-2024-24549")));
        given(cveRepository.findByCveId("CVE-2024-24549"))
                .willReturn(Optional.of(Cve.builder()
                        .cveId("CVE-2024-24549")
                        .build()));
        given(findingRepository
                .existsByScanIdAndComponentOccurrenceIdAndCveCveId(
                        SCAN_ID, occ.getId(), "CVE-2024-24549"))
                .willReturn(true);

        listener.onScanIngested(new ScanIngestedEvent(SCAN_ID, UUID.randomUUID(), null, 1, 0, java.time.Instant.now()));

        verify(findingRepository, never()).save(any(Finding.class));
    }

    @Test
    @DisplayName("Occurrences ohne PURL werden ueberspringen")
    void skipWithoutPurl() {
        given(lookup.isEnabled()).willReturn(true);
        ComponentOccurrence occ = occurrence(null, UUID.randomUUID());
        given(occurrenceRepository.findByScanId(SCAN_ID))
                .willReturn(List.of(occ));

        listener.onScanIngested(new ScanIngestedEvent(SCAN_ID, UUID.randomUUID(), null, 1, 0, java.time.Instant.now()));

        verify(lookup, never()).findCveIdsForPurls(anyList());
        verify(findingRepository, never()).save(any(Finding.class));
    }

    private static ComponentOccurrence occurrence(String purl, UUID id) {
        Component c = Component.builder()
                .id(UUID.randomUUID())
                .purl(purl)
                .name(purl != null ? purl : "unknown")
                .version("1.0.0")
                .build();
        Scan scan = Scan.builder().id(SCAN_ID).build();
        return ComponentOccurrence.builder()
                .id(id)
                .scan(scan)
                .component(c)
                .direct(Boolean.TRUE)
                .build();
    }
}
