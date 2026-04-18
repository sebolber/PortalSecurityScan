package com.ahs.cvm.ai.fixverify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.ai.fixverify.OpenFixWatchdog.WatchdogReport;
import com.ahs.cvm.integration.git.GitProviderPort;
import com.ahs.cvm.integration.git.GitProviderPort.ReleaseNotes;
import com.ahs.cvm.persistence.ai.AiCallAuditRepository;
import com.ahs.cvm.persistence.ai.AiSourceRef;
import com.ahs.cvm.persistence.ai.AiSourceRefRepository;
import com.ahs.cvm.persistence.ai.AiSuggestion;
import com.ahs.cvm.persistence.ai.AiSuggestionRepository;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenFixWatchdogTest {

    private FindingRepository findingRepo;
    private AssessmentRepository assessmentRepo;
    private GitProviderPort provider;
    private AiSuggestionRepository suggestionRepo;
    private AiSourceRefRepository sourceRefRepo;
    private AiCallAuditRepository auditRepo;
    private OpenFixWatchdog watchdog;

    @BeforeEach
    void setUp() {
        findingRepo = mock(FindingRepository.class);
        assessmentRepo = mock(AssessmentRepository.class);
        provider = mock(GitProviderPort.class);
        suggestionRepo = mock(AiSuggestionRepository.class);
        sourceRefRepo = mock(AiSourceRefRepository.class);
        auditRepo = mock(AiCallAuditRepository.class);
        given(auditRepo.findByStatusAndCreatedAtBefore(any(), any()))
                .willReturn(List.of());
        given(auditRepo.save(any())).willAnswer(inv -> {
            com.ahs.cvm.persistence.ai.AiCallAudit a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        given(suggestionRepo.save(any(AiSuggestion.class))).willAnswer(inv -> {
            AiSuggestion s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        given(suggestionRepo.findByFindingId(any())).willReturn(List.of());
        given(assessmentRepo.findByFindingIdOrderByVersionAsc(any())).willReturn(List.of(
                Assessment.builder().id(UUID.randomUUID()).build()));
        watchdog = new OpenFixWatchdog(
                new FixVerificationConfig(true, 50, 1440),
                findingRepo, assessmentRepo, provider,
                suggestionRepo, sourceRefRepo, auditRepo, true);
    }

    private Finding finding(String cveKey, String fixedInVersion) {
        Cve c = Cve.builder().id(UUID.randomUUID()).cveId(cveKey).build();
        return Finding.builder()
                .id(UUID.randomUUID())
                .cve(c)
                .fixedInVersion(fixedInVersion)
                .build();
    }

    @Test
    @DisplayName("Watchdog: Finding ohne Fix und mit neuem Release -> Upgrade-Suggestion + NEEDS_REVIEW")
    void upgradeErkannt() {
        Finding f = finding("CVE-X", null);
        given(findingRepo.findAll()).willReturn(List.of(f));
        given(provider.releaseNotes(anyString(), any())).willReturn(Optional.of(
                new ReleaseNotes("https://github.com/foo/bar", "v1.2.3",
                        "Various fixes", Instant.now(),
                        "https://github.com/foo/bar/releases/v1.2.3")));

        WatchdogReport report = watchdog.runOnce(Map.of("CVE-X", "https://github.com/foo/bar"));

        assertThat(report.neueVorschlaege()).isEqualTo(1);
        verify(suggestionRepo).save(any(AiSuggestion.class));
        verify(sourceRefRepo).save(any(AiSourceRef.class));
        verify(assessmentRepo).markiereAlsReview(any(), any());
    }

    @Test
    @DisplayName("Watchdog: Finding mit fixedInVersion wird uebersprungen")
    void findingMitFix() {
        Finding f = finding("CVE-X", "v2.0.0");
        given(findingRepo.findAll()).willReturn(List.of(f));

        WatchdogReport report = watchdog.runOnce(Map.of("CVE-X", "https://github.com/foo/bar"));

        assertThat(report.geprueft()).isZero();
        verify(provider, never()).releaseNotes(any(), any());
    }

    @Test
    @DisplayName("Watchdog: kein Release vom Provider -> kein Vorschlag")
    void keinRelease() {
        Finding f = finding("CVE-X", null);
        given(findingRepo.findAll()).willReturn(List.of(f));
        given(provider.releaseNotes(anyString(), any())).willReturn(Optional.empty());

        WatchdogReport report = watchdog.runOnce(Map.of("CVE-X", "https://github.com/foo/bar"));

        assertThat(report.neueVorschlaege()).isZero();
        verify(suggestionRepo, never()).save(any(AiSuggestion.class));
    }

    @Test
    @DisplayName("Watchdog: Vorschlag fuer gleichen Tag wird nicht doppelt angelegt")
    void dedup() {
        Finding f = finding("CVE-X", null);
        given(findingRepo.findAll()).willReturn(List.of(f));
        given(provider.releaseNotes(anyString(), any())).willReturn(Optional.of(
                new ReleaseNotes("r", "v2.0.0", "body", Instant.now(), "u")));
        given(suggestionRepo.findByFindingId(f.getId())).willReturn(List.of(
                AiSuggestion.builder()
                        .useCase("UPGRADE_RECOMMENDED")
                        .rationale("Upstream-Release v2.0.0 verfuegbar.")
                        .build()));

        WatchdogReport report = watchdog.runOnce(Map.of("CVE-X", "r"));

        assertThat(report.neueVorschlaege()).isZero();
        verify(suggestionRepo, never()).save(any(AiSuggestion.class));
    }

    @Test
    @DisplayName("Watchdog: deaktiviert -> runOnce laeuft manuell, scheduled-Run tut nichts")
    void deaktiviert() {
        watchdog = new OpenFixWatchdog(
                new FixVerificationConfig(false, 50, 1440),
                findingRepo, assessmentRepo, provider,
                suggestionRepo, sourceRefRepo, auditRepo, true);

        watchdog.scheduledRun();

        verify(findingRepo, never()).findAll();
    }
}
