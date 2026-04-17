package com.ahs.cvm.application.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.application.cascade.CascadeInput;
import com.ahs.cvm.application.cascade.CascadeOutcome;
import com.ahs.cvm.application.cascade.CascadeService;
import com.ahs.cvm.application.profile.ContextProfileService;
import com.ahs.cvm.application.scan.ScanIngestedEvent;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.scan.Component;
import com.ahs.cvm.persistence.scan.ComponentOccurrence;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FindingsCreatedListenerTest {

    private FindingRepository findingRepository;
    private ContextProfileService profileService;
    private CascadeService cascadeService;
    private AssessmentWriteService writeService;
    private FindingsCreatedListener listener;

    private final UUID scanId = UUID.randomUUID();
    private final UUID productVersionId = UUID.randomUUID();
    private final UUID environmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        findingRepository = mock(FindingRepository.class);
        profileService = mock(ContextProfileService.class);
        cascadeService = mock(CascadeService.class);
        writeService = mock(AssessmentWriteService.class);
        listener = new FindingsCreatedListener(
                findingRepository, profileService, cascadeService, writeService);
        given(profileService.latestActiveFor(any())).willReturn(Optional.empty());
    }

    @Test
    @DisplayName("Scan-Ingest: pro Finding wird Cascade aufgerufen und PROPOSED-Vorschlag persistiert")
    void cascadeFuerJedesFinding() {
        Finding f1 = finding("CVE-2017-18640");
        Finding f2 = finding("CVE-2025-48924");
        given(findingRepository.findByScanId(scanId)).willReturn(List.of(f1, f2));
        given(cascadeService.bewerte(any(CascadeInput.class)))
                .willReturn(CascadeOutcome.rule(
                        UUID.randomUUID(), AhsSeverity.LOW, "Regel trifft", List.of()));

        listener.onScanIngested(new ScanIngestedEvent(
                scanId, productVersionId, environmentId, 5, 2, Instant.now()));

        ArgumentCaptor<AssessmentWriteService.ProposeCommand> cmd =
                ArgumentCaptor.forClass(AssessmentWriteService.ProposeCommand.class);
        verify(writeService, times(2)).propose(cmd.capture());
        assertThat(cmd.getAllValues())
                .extracting(AssessmentWriteService.ProposeCommand::source)
                .containsOnly(ProposalSource.RULE);
    }

    @Test
    @DisplayName("Listener verschluckt Cascade-Fehler und macht beim naechsten Finding weiter")
    void fehlertoleranz() {
        Finding f1 = finding("CVE-2017-18640");
        Finding f2 = finding("CVE-2025-48924");
        given(findingRepository.findByScanId(scanId)).willReturn(List.of(f1, f2));
        given(cascadeService.bewerte(any()))
                .willThrow(new RuntimeException("kaputt"))
                .willReturn(CascadeOutcome.manual());

        listener.onScanIngested(new ScanIngestedEvent(
                scanId, productVersionId, environmentId, 1, 1, Instant.now()));

        verify(writeService, times(1)).propose(any());
    }

    private Finding finding(String cveId) {
        Component component = Component.builder()
                .id(UUID.randomUUID())
                .name("lib")
                .version("1.0")
                .type("maven")
                .purl("pkg:maven/com/lib@1.0")
                .build();
        ComponentOccurrence occ = ComponentOccurrence.builder()
                .id(UUID.randomUUID()).component(component).build();
        return Finding.builder()
                .id(UUID.randomUUID())
                .cve(Cve.builder().id(UUID.randomUUID()).cveId(cveId).build())
                .componentOccurrence(occ)
                .detectedAt(Instant.now())
                .build();
    }
}
