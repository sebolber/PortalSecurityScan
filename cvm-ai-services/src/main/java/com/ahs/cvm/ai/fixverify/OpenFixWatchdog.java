package com.ahs.cvm.ai.fixverify;

import com.ahs.cvm.domain.enums.AiCallStatus;
import com.ahs.cvm.integration.git.GitProviderPort;
import com.ahs.cvm.integration.git.GitProviderPort.ReleaseNotes;
import com.ahs.cvm.persistence.ai.AiCallAudit;
import com.ahs.cvm.persistence.ai.AiCallAuditRepository;
import com.ahs.cvm.persistence.ai.AiSourceRef;
import com.ahs.cvm.persistence.ai.AiSourceRefRepository;
import com.ahs.cvm.persistence.ai.AiSuggestion;
import com.ahs.cvm.persistence.ai.AiSuggestionRepository;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Taeglicher Scheduler: prueft offene Findings ohne Fix auf neue
 * Upstream-Releases. Liefert der Provider etwas Neues, wird ein
 * {@link AiSuggestion} vom Typ {@code UPGRADE_RECOMMENDED} angelegt
 * und das zugehoerige aktive Assessment auf
 * {@link com.ahs.cvm.domain.enums.AssessmentStatus#NEEDS_REVIEW}
 * gehoben (Iteration 16, CVM-41).
 *
 * <p>Der Cron laeuft nur, wenn {@code cvm.ai.fix-verification.enabled=true}
 * und {@code cvm.scheduler.enabled=true}. Tests rufen
 * {@link #runOnce(Map)} direkt auf.
 */
@Component
public class OpenFixWatchdog {

    private static final Logger log = LoggerFactory.getLogger(OpenFixWatchdog.class);
    private static final String USE_CASE = "UPGRADE_RECOMMENDED";

    private final FixVerificationConfig config;
    private final FindingRepository findingRepository;
    private final AssessmentRepository assessmentRepository;
    private final GitProviderPort gitProvider;
    private final AiSuggestionRepository suggestionRepository;
    private final AiSourceRefRepository sourceRefRepository;
    private final AiCallAuditRepository auditRepository;
    private final boolean schedulerEnabled;

    public OpenFixWatchdog(
            FixVerificationConfig config,
            FindingRepository findingRepository,
            AssessmentRepository assessmentRepository,
            GitProviderPort gitProvider,
            AiSuggestionRepository suggestionRepository,
            AiSourceRefRepository sourceRefRepository,
            AiCallAuditRepository auditRepository,
            @Value("${cvm.scheduler.enabled:false}") boolean schedulerEnabled) {
        this.config = config;
        this.findingRepository = findingRepository;
        this.assessmentRepository = assessmentRepository;
        this.gitProvider = gitProvider;
        this.suggestionRepository = suggestionRepository;
        this.sourceRefRepository = sourceRefRepository;
        this.auditRepository = auditRepository;
        this.schedulerEnabled = schedulerEnabled;
    }

    /** Tageslauf. */
    @Scheduled(cron = "${cvm.ai.fix-verification.watchdog-cron:0 0 4 * * *}")
    @Transactional
    public void scheduledRun() {
        if (!config.enabled() || !schedulerEnabled) {
            return;
        }
        runOnce(Map.of());
    }

    /**
     * Einmaliger Durchlauf fuer alle Findings ohne {@code fixedInVersion}.
     * Die {@code repoUrls} Map mappt {@code cveKey -&gt; repoUrl}. In Prod
     * wird die Map aus einer Configuration / DB-Sicht gefuellt; aktuell
     * ruft der Admin-Endpunkt sie mit einer expliziten Map auf.
     */
    @Transactional
    public WatchdogReport runOnce(Map<String, String> repoUrls) {
        int geprueft = 0;
        int neueVorschlaege = 0;
        Map<UUID, String> updates = new HashMap<>();

        for (Finding finding : findingRepository.findAll()) {
            if (finding.getFixedInVersion() != null
                    && !finding.getFixedInVersion().isBlank()) {
                continue;
            }
            geprueft++;
            String repoUrl = repoUrls.get(finding.getCve().getCveId());
            if (repoUrl == null || repoUrl.isBlank()) {
                continue;
            }
            Optional<ReleaseNotes> latest = gitProvider.releaseNotes(repoUrl, "latest");
            if (latest.isEmpty()) {
                continue;
            }
            String aktiveAssessmentMarker = findingMarker(finding);
            if (alreadyRecommended(finding.getId(), latest.get().tag())) {
                continue;
            }
            neueVorschlaege++;
            updates.put(finding.getId(), latest.get().tag());
            erzeugeUpgradeEmpfehlung(finding, latest.get());
            setzeAssessmentsAufReview(finding.getId());
            log.info("Watchdog: Upgrade-Empfehlung {} -> {} fuer CVE {}",
                    aktiveAssessmentMarker, latest.get().tag(),
                    finding.getCve().getCveId());
        }
        return new WatchdogReport(geprueft, neueVorschlaege, updates);
    }

    private boolean alreadyRecommended(UUID findingId, String tag) {
        return suggestionRepository.findByFindingId(findingId).stream()
                .anyMatch(s -> USE_CASE.equals(s.getUseCase())
                        && s.getRationale() != null
                        && s.getRationale().contains(tag));
    }

    private void erzeugeUpgradeEmpfehlung(Finding finding, ReleaseNotes release) {
        AiCallAudit audit = auditRepository
                .findByStatusAndCreatedAtBefore(AiCallStatus.OK, Instant.now())
                .stream()
                .filter(a -> USE_CASE.equals(a.getUseCase()))
                .reduce((a, b) -> b)
                .orElseGet(() -> synthetischerAuditEintrag(release));

        AiSuggestion suggestion = suggestionRepository.save(AiSuggestion.builder()
                .aiCallAudit(audit)
                .useCase(USE_CASE)
                .finding(finding)
                .cve(finding.getCve())
                .severity(null)
                .rationale("Upstream-Release " + release.tag()
                        + " verfuegbar. Bitte pruefen, ob der Fix "
                        + finding.getCve().getCveId() + " adressiert.")
                .build());
        sourceRefRepository.save(AiSourceRef.builder()
                .aiSuggestion(suggestion)
                .kind("DOCUMENT")
                .reference(release.url())
                .excerpt(firstChars(release.body(), 500))
                .build());
    }

    private AiCallAudit synthetischerAuditEintrag(ReleaseNotes release) {
        // Watchdog laeuft ohne LLM-Call; damit AiSuggestion den NOT-NULL-FK
        // auf ai_call_audit befriedigt, legen wir einen synthetischen
        // OK-Eintrag an. Offener Punkt: Subprocess-/Watchdog-Audit-
        // Lightweight (siehe offene-punkte.md Iteration 16).
        AiCallAudit audit = AiCallAudit.builder()
                .useCase(USE_CASE)
                .modelId("watchdog")
                .promptTemplateId("watchdog")
                .promptTemplateVersion("v1")
                .systemPrompt("OpenFixWatchdog scheduled run")
                .userPrompt("release=" + (release == null ? "" : release.tag()))
                .triggeredBy("system:open-fix-watchdog")
                .injectionRisk(false)
                .status(AiCallStatus.OK)
                .finalizingAllowed(true)
                .createdAt(Instant.now())
                .finalizedAt(Instant.now())
                .build();
        return auditRepository.save(audit);
    }

    private void setzeAssessmentsAufReview(UUID findingId) {
        List<Assessment> aktive = assessmentRepository.findByFindingIdOrderByVersionAsc(findingId)
                .stream()
                .filter(a -> a.getSupersededAt() == null)
                .toList();
        if (aktive.isEmpty()) {
            return;
        }
        List<UUID> ids = aktive.stream().map(Assessment::getId).toList();
        // Nutze den bestehenden @Modifying-Query, der State-Machine-konform
        // arbeitet. ProfileVersion = null, weil der Re-Review hier nicht
        // aus einer Profil-Aenderung kommt.
        assessmentRepository.markiereAlsReview(ids, null);
    }

    private static String findingMarker(Finding f) {
        return f.getComponentOccurrence() == null
                ? f.getId().toString()
                : f.getComponentOccurrence().getComponent().getName() + "@"
                        + f.getComponentOccurrence().getComponent().getVersion();
    }

    private static String firstChars(String s, int limit) {
        if (s == null) {
            return "";
        }
        return s.length() > limit ? s.substring(0, limit) + "..." : s;
    }

    /** Minimal-Report fuer Admin-Calls und Tests. */
    public record WatchdogReport(
            int geprueft,
            int neueVorschlaege,
            Map<UUID, String> updates) {

        public WatchdogReport {
            updates = updates == null ? Map.of() : Map.copyOf(updates);
        }
    }
}
