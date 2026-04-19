package com.ahs.cvm.ai.fixverify;

import com.ahs.cvm.ai.fixverify.SuspiciousCommitHeuristic.Verdict;
import com.ahs.cvm.domain.enums.AiCallStatus;
import com.ahs.cvm.domain.enums.FixEvidenceType;
import com.ahs.cvm.domain.enums.FixVerificationGrade;
import com.ahs.cvm.integration.git.GitProviderPort;
import com.ahs.cvm.integration.git.GitProviderPort.CommitSummary;
import com.ahs.cvm.integration.git.GitProviderPort.ReleaseNotes;
import com.ahs.cvm.llm.AiCallAuditService;
import com.ahs.cvm.llm.LlmClient;
import com.ahs.cvm.llm.LlmClient.LlmRequest;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClient.Message;
import com.ahs.cvm.llm.LlmClientSelector;
import com.ahs.cvm.llm.prompt.PromptTemplate;
import com.ahs.cvm.llm.prompt.PromptTemplateLoader;
import com.ahs.cvm.persistence.ai.AiCallAudit;
import com.ahs.cvm.persistence.ai.AiCallAuditRepository;
import com.ahs.cvm.persistence.ai.AiSourceRef;
import com.ahs.cvm.persistence.ai.AiSourceRefRepository;
import com.ahs.cvm.persistence.ai.AiSuggestion;
import com.ahs.cvm.persistence.ai.AiSuggestionRepository;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.mitigation.MitigationPlan;
import com.ahs.cvm.persistence.mitigation.MitigationPlanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestriert die LLM-gestuetzte Fix-Verifikation
 * (Iteration 16, CVM-41).
 *
 * <p>Grade A wird vom Service nur dann durchgelassen, wenn die lokale
 * Heuristik die CVE-ID in Release-Notes oder Commit-Message
 * tatsaechlich gefunden hat. Damit bleibt die Grade-Semantik stabil,
 * auch wenn das LLM zu optimistisch klassifiziert.
 */
@Service
public class FixVerificationService {

    private static final Logger log = LoggerFactory.getLogger(FixVerificationService.class);
    private static final String USE_CASE = "FIX_VERIFICATION";
    private static final String TEMPLATE_ID = "fix-verification";
    private static final Pattern CVE_ID = Pattern.compile("CVE-\\d{4}-\\d{4,7}");
    private static final Pattern GHSA_ID =
            Pattern.compile("GHSA(-[a-z0-9]{4}){3}", Pattern.CASE_INSENSITIVE);

    private final FixVerificationConfig config;
    private final MitigationPlanRepository mitigationRepository;
    private final GitProviderPort gitProvider;
    private final SuspiciousCommitHeuristic heuristic;
    private final LlmClientSelector clientSelector;
    private final AiCallAuditService auditService;
    private final AiCallAuditRepository auditRepository;
    private final AiSuggestionRepository suggestionRepository;
    private final AiSourceRefRepository sourceRefRepository;
    private final PromptTemplateLoader templateLoader;
    private final ConcurrentHashMap<String, CachedProviderResponse> cache =
            new ConcurrentHashMap<>();

    public FixVerificationService(
            FixVerificationConfig config,
            MitigationPlanRepository mitigationRepository,
            GitProviderPort gitProvider,
            SuspiciousCommitHeuristic heuristic,
            LlmClientSelector clientSelector,
            AiCallAuditService auditService,
            AiCallAuditRepository auditRepository,
            AiSuggestionRepository suggestionRepository,
            AiSourceRefRepository sourceRefRepository,
            PromptTemplateLoader templateLoader) {
        this.config = config;
        this.mitigationRepository = mitigationRepository;
        this.gitProvider = gitProvider;
        this.heuristic = heuristic;
        this.clientSelector = clientSelector;
        this.auditService = auditService;
        this.auditRepository = auditRepository;
        this.suggestionRepository = suggestionRepository;
        this.sourceRefRepository = sourceRefRepository;
        this.templateLoader = templateLoader;
    }

    @Transactional
    public FixVerificationResult verify(FixVerificationRequest request) {
        if (!config.enabledEffective()) {
            return unavailable(request.mitigationId(),
                    "Fix-Verifikation deaktiviert.");
        }
        MitigationPlan plan = mitigationRepository.findById(request.mitigationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mitigation nicht gefunden: " + request.mitigationId()));
        Assessment assessment = plan.getAssessment();
        Cve cve = assessment.getCve();
        String cveKey = cve.getCveId();

        String fromVersion = request.fromVersion();
        String toVersion = request.toVersion() != null
                ? request.toVersion() : plan.getTargetVersion();
        if (fromVersion == null || fromVersion.isBlank()
                || toVersion == null || toVersion.isBlank()) {
            return unavailable(request.mitigationId(),
                    "Versionen fehlen (fromVersion/toVersion).");
        }

        String repoUrl = request.repoUrl();
        if (repoUrl == null || repoUrl.isBlank()) {
            return unavailable(request.mitigationId(), "repoUrl fehlt.");
        }

        CachedProviderResponse providerData = loadMitCache(repoUrl, fromVersion, toVersion);
        List<CommitSummary> commits = providerData.commits;
        ReleaseNotes notes = providerData.notes;

        List<Verdict> verdicts = new ArrayList<>();
        for (CommitSummary c : commits) {
            verdicts.add(heuristic.classify(c, request.vulnerableSymbol(), cveKey));
        }
        List<Verdict> suspicious = verdicts.stream()
                .filter(Verdict::suspicious).toList();
        List<Verdict> rest = verdicts.stream()
                .filter(v -> !v.suspicious()).toList();

        boolean messagesOnly = commits.size() > config.fullTextCommitCapEffective();

        LlmResponse response;
        try {
            response = runLlm(request, cve, fromVersion, toVersion, notes,
                    suspicious, rest, messagesOnly);
        } catch (RuntimeException ex) {
            return unavailable(request.mitigationId(),
                    "LLM-Call fehlgeschlagen: " + ex.getMessage());
        }

        JsonNode out = response.structuredOutput();
        FixVerificationGrade llmGrade = parseGrade(out);
        FixEvidenceType evidenceType = parseEvidence(out);
        BigDecimal confidence = parseConfidence(out);
        List<FixVerificationResult.CommitEvidence> evidence = parseEvidenceList(out);
        List<String> caveats = parseCaveats(out);

        boolean cveExplicit = hatCveMention(cveKey, notes, commits);
        FixVerificationGrade finalGrade = llmGrade;
        if (finalGrade == FixVerificationGrade.A && !cveExplicit) {
            finalGrade = suspicious.isEmpty()
                    ? FixVerificationGrade.C : FixVerificationGrade.B;
            evidenceType = suspicious.isEmpty()
                    ? FixEvidenceType.NONE : FixEvidenceType.FIX_COMMIT_MATCH;
            caveats = prepend(caveats,
                    "Service-Downgrade: Grade A verworfen, weil CVE-ID in keiner "
                            + "Quelle belegt.");
        }
        if (cveExplicit && finalGrade == FixVerificationGrade.C) {
            finalGrade = FixVerificationGrade.A;
            evidenceType = FixEvidenceType.EXPLICIT_CVE_MENTION;
            caveats = prepend(caveats,
                    "Service-Upgrade: CVE-ID in Release-Notes oder Commit nachgewiesen.");
        }

        // Audit-Id via "letzter OK-Eintrag dieses Use-Case" (gleiches
        // Pattern wie AutoAssessmentOrchestrator, siehe offene-punkte.md).
        AiCallAudit audit = auditRepository
                .findByStatusAndCreatedAtBefore(AiCallStatus.OK, Instant.now())
                .stream()
                .filter(a -> USE_CASE.equals(a.getUseCase()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException(
                        "Kein OK-Audit-Eintrag fuer Use-Case " + USE_CASE));
        AiSuggestion suggestion = suggestionRepository.save(AiSuggestion.builder()
                .aiCallAudit(audit)
                .useCase(USE_CASE)
                .finding(assessment.getFinding())
                .cve(cve)
                .severity(null)
                .rationale("Fix-Verifikation Grade " + finalGrade + ": "
                        + String.join("; ", caveats))
                .confidence(confidence)
                .build());

        List<FixVerificationResult.CommitEvidence> persistierteEvidenz = new ArrayList<>();
        for (FixVerificationResult.CommitEvidence e : evidence) {
            sourceRefRepository.save(AiSourceRef.builder()
                    .aiSuggestion(suggestion)
                    .kind("GIT_COMMIT")
                    .reference(e.commit())
                    .excerpt(e.message() == null ? "" : e.message())
                    .build());
            persistierteEvidenz.add(e);
        }

        plan.setVerificationGrade(finalGrade);
        plan.setVerificationEvidenceType(evidenceType);
        plan.setVerifiedAt(Instant.now());
        mitigationRepository.save(plan);

        return new FixVerificationResult(
                request.mitigationId(),
                suggestion.getId(),
                finalGrade,
                evidenceType,
                confidence,
                persistierteEvidenz,
                caveats,
                plan.getVerifiedAt(),
                true,
                null);
    }

    /** Rein lesende Sicht auf den aktuellen Stand im Mitigation-Plan. */
    @Transactional(readOnly = true)
    public FixVerificationResult load(UUID mitigationId) {
        MitigationPlan plan = mitigationRepository.findById(mitigationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mitigation nicht gefunden: " + mitigationId));
        FixVerificationGrade grade = plan.getVerificationGrade() == null
                ? FixVerificationGrade.UNKNOWN : plan.getVerificationGrade();
        FixEvidenceType type = plan.getVerificationEvidenceType() == null
                ? FixEvidenceType.NONE : plan.getVerificationEvidenceType();
        boolean vorhanden = plan.getVerifiedAt() != null;
        return new FixVerificationResult(
                mitigationId, null, grade, type, BigDecimal.ZERO,
                List.of(), List.of(), plan.getVerifiedAt(),
                vorhanden,
                vorhanden ? null : "Keine Verifikation durchgefuehrt.");
    }

    private CachedProviderResponse loadMitCache(String repoUrl, String fromVersion,
            String toVersion) {
        String key = repoUrl + "|" + fromVersion + "..." + toVersion;
        CachedProviderResponse cached = cache.get(key);
        Instant now = Instant.now();
        if (cached != null && Duration.between(cached.fetchedAt, now)
                .toMinutes() < config.cacheTtlMinutesEffective()) {
            return cached;
        }
        ReleaseNotes notes = gitProvider.releaseNotes(repoUrl, toVersion).orElse(null);
        List<CommitSummary> commits = gitProvider.compare(repoUrl, fromVersion, toVersion);
        CachedProviderResponse fresh = new CachedProviderResponse(notes, commits, now);
        cache.put(key, fresh);
        return fresh;
    }

    /**
     * Entfernt Cache-Eintraege, die aelter als {@code 2 * cacheTtlMinutes}
     * sind. Wird taeglich vom {@link FixVerificationCacheEvictionJob}
     * aufgerufen, damit der Speicher bei laufendem Betrieb nicht
     * unbegrenzt waechst.
     */
    public int purgeExpiredCache() {
        long grenzeMin = config.cacheTtlMinutesEffective() * 2L;
        Instant now = Instant.now();
        int vorher = cache.size();
        cache.entrySet().removeIf(e -> Duration.between(
                e.getValue().fetchedAt, now).toMinutes() >= grenzeMin);
        int entfernt = vorher - cache.size();
        if (entfernt > 0) {
            log.info("FixVerification-Cache purged: {} Eintraege entfernt, {} verbleiben.",
                    entfernt, cache.size());
        }
        return entfernt;
    }

    /** Paketsichtbar fuer Tests. */
    int cacheSize() {
        return cache.size();
    }

    private LlmResponse runLlm(FixVerificationRequest request, Cve cve,
            String fromVersion, String toVersion, ReleaseNotes notes,
            List<Verdict> suspicious, List<Verdict> rest, boolean messagesOnly) {
        PromptTemplate template = templateLoader.load(TEMPLATE_ID);
        Map<String, Object> vars = new HashMap<>();
        vars.put("cveKey", cve.getCveId());
        vars.put("vulnerableSymbol",
                request.vulnerableSymbol() == null ? "(nicht angegeben)"
                        : request.vulnerableSymbol());
        vars.put("fromVersion", fromVersion);
        vars.put("toVersion", toVersion);
        vars.put("releaseNotes", notes == null
                ? "(keine Release-Notes verfuegbar)"
                : notes.body());
        vars.put("suspiciousCommits", formatiereVerdicts(suspicious, false));
        vars.put("otherCommits", messagesOnly
                ? formatiereVerdicts(rest, true)
                : formatiereVerdicts(rest, false));
        String userPrompt = template.renderUser(vars);
        String systemPrompt = template.renderSystem(Map.of());

        LlmClient client = clientSelector.select(null, USE_CASE);
        LlmRequest req = new LlmRequest(
                USE_CASE, template.id(), template.version(),
                systemPrompt,
                List.of(new Message(Message.Role.USER, userPrompt)),
                null, 0.1, 1024, null, request.triggeredBy(),
                null, Map.of("messagesOnly", messagesOnly));
        return auditService.execute(client, req);
    }

    private static String formatiereVerdicts(List<Verdict> verdicts, boolean messagesOnly) {
        if (verdicts.isEmpty()) {
            return "(keine Commits)";
        }
        return verdicts.stream().map(v -> {
            CommitSummary c = v.commit();
            if (messagesOnly) {
                return "- " + c.sha().substring(0, Math.min(8, c.sha().length()))
                        + " " + firstLine(c.message());
            }
            return "- " + c.sha() + " [" + v.reason() + "] "
                    + firstLine(c.message()) + " ("
                    + String.join(", ", c.filesTouched()) + ")";
        }).collect(Collectors.joining("\n"));
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "";
        }
        int nl = message.indexOf('\n');
        String head = nl < 0 ? message : message.substring(0, nl);
        return head.length() > 200 ? head.substring(0, 200) + "..." : head;
    }

    static FixVerificationGrade parseGrade(JsonNode out) {
        if (out == null) {
            return FixVerificationGrade.UNKNOWN;
        }
        String v = out.path("quality").asText("");
        try {
            return FixVerificationGrade.valueOf(v);
        } catch (IllegalArgumentException ex) {
            return FixVerificationGrade.UNKNOWN;
        }
    }

    static FixEvidenceType parseEvidence(JsonNode out) {
        if (out == null) {
            return FixEvidenceType.NONE;
        }
        String v = out.path("evidenceType").asText("");
        try {
            return FixEvidenceType.valueOf(v);
        } catch (IllegalArgumentException ex) {
            return FixEvidenceType.NONE;
        }
    }

    static BigDecimal parseConfidence(JsonNode out) {
        if (out == null || !out.has("confidence") || !out.get("confidence").isNumber()) {
            return BigDecimal.ZERO;
        }
        double d = Math.max(0.0, Math.min(1.0, out.get("confidence").asDouble()));
        return BigDecimal.valueOf(d).setScale(3, RoundingMode.HALF_UP);
    }

    static List<FixVerificationResult.CommitEvidence> parseEvidenceList(JsonNode out) {
        if (out == null || !out.has("adressedBy") || !out.get("adressedBy").isArray()) {
            return List.of();
        }
        List<FixVerificationResult.CommitEvidence> list = new ArrayList<>();
        for (JsonNode e : out.get("adressedBy")) {
            list.add(new FixVerificationResult.CommitEvidence(
                    e.path("commit").asText(""),
                    e.path("message").asText(""),
                    e.path("url").asText("")));
        }
        return List.copyOf(list);
    }

    static List<String> parseCaveats(JsonNode out) {
        if (out == null || !out.has("caveats") || !out.get("caveats").isArray()) {
            return new ArrayList<>();
        }
        List<String> list = new ArrayList<>();
        for (JsonNode c : out.get("caveats")) {
            if (c.isTextual()) {
                list.add(c.asText());
            }
        }
        return list;
    }

    static boolean hatCveMention(String cveKey, ReleaseNotes notes,
            List<CommitSummary> commits) {
        if (cveKey == null || cveKey.isBlank()) {
            return false;
        }
        if (notes != null && notes.body() != null && notes.body().contains(cveKey)) {
            return true;
        }
        for (CommitSummary c : commits) {
            if (c.message() != null && c.message().contains(cveKey)) {
                return true;
            }
            if (c.message() != null && CVE_ID.matcher(c.message()).find()
                    && c.message().toLowerCase(Locale.ROOT)
                            .contains(cveKey.toLowerCase(Locale.ROOT))) {
                return true;
            }
            if (c.message() != null && GHSA_ID.matcher(c.message()).find()) {
                return true;
            }
        }
        return false;
    }

    private static List<String> prepend(List<String> list, String head) {
        List<String> out = new ArrayList<>(list.size() + 1);
        out.add(head);
        out.addAll(list);
        return out;
    }

    private FixVerificationResult unavailable(UUID id, String note) {
        log.info("Fix-Verifikation {} nicht verfuegbar: {}", id, note);
        return new FixVerificationResult(
                id, null, FixVerificationGrade.UNKNOWN,
                FixEvidenceType.NONE, BigDecimal.ZERO,
                List.of(), List.of(note), null, false, note);
    }

    private record CachedProviderResponse(
            ReleaseNotes notes,
            List<CommitSummary> commits,
            Instant fetchedAt) {}

    /** Eingabe-Record fuer {@link #verify(FixVerificationRequest)}. */
    public record FixVerificationRequest(
            UUID mitigationId,
            String repoUrl,
            String fromVersion,
            String toVersion,
            String vulnerableSymbol,
            String triggeredBy) {

        public FixVerificationRequest {
            if (mitigationId == null) {
                throw new IllegalArgumentException("mitigationId darf nicht null sein.");
            }
            if (triggeredBy == null || triggeredBy.isBlank()) {
                throw new IllegalArgumentException("triggeredBy darf nicht leer sein.");
            }
        }
    }
}
