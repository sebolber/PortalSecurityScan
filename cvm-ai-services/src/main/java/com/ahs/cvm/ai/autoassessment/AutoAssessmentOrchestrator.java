package com.ahs.cvm.ai.autoassessment;

import com.ahs.cvm.ai.rag.IndexingService;
import com.ahs.cvm.ai.rag.RetrievalService;
import com.ahs.cvm.ai.rag.RetrievalService.RagHit;
import com.ahs.cvm.application.cascade.AiAssessmentSuggesterPort;
import com.ahs.cvm.application.cascade.CascadeInput;
import com.ahs.cvm.application.cascade.CascadeOutcome;
import com.ahs.cvm.application.rules.RuleEvaluationContext;
import com.ahs.cvm.application.rules.RuleEvaluationContext.ComponentSnapshot;
import com.ahs.cvm.application.rules.RuleEvaluationContext.CveSnapshot;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.llm.AiCallAuditService;
import com.ahs.cvm.llm.LlmClient;
import com.ahs.cvm.llm.LlmClient.LlmRequest;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClient.Message;
import com.ahs.cvm.llm.LlmClientSelector;
import com.ahs.cvm.llm.LlmDisabledException;
import com.ahs.cvm.llm.prompt.PromptTemplate;
import com.ahs.cvm.llm.prompt.PromptTemplateLoader;
import com.ahs.cvm.persistence.ai.AiCallAudit;
import com.ahs.cvm.persistence.ai.AiCallAuditRepository;
import com.ahs.cvm.persistence.ai.AiSourceRef;
import com.ahs.cvm.persistence.ai.AiSourceRefRepository;
import com.ahs.cvm.persistence.ai.AiSuggestion;
import com.ahs.cvm.persistence.ai.AiSuggestionRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestriert die KI-Vorbewertung (Cascade-Stufe&nbsp;3).
 *
 * <p>Fluss: RAG-Query (Top-K aus {@link RetrievalService}) -&gt;
 * Prompt-Rendering -&gt; LLM-Call ueber {@link AiCallAuditService}
 * -&gt; Halluzinations-Check -&gt; konservativer Default -&gt;
 * Persistenz {@link AiSuggestion} + {@link AiSourceRef}.
 *
 * <p>Implementiert {@link AiAssessmentSuggesterPort}, sodass der
 * {@link com.ahs.cvm.application.cascade.CascadeService} die
 * AI-Stufe ohne direkte Modul-Kopplung aufrufen kann.
 */
@Component
public class AutoAssessmentOrchestrator implements AiAssessmentSuggesterPort {

    private static final Logger log = LoggerFactory.getLogger(AutoAssessmentOrchestrator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String USE_CASE = "AUTO_ASSESSMENT";
    private static final String PROMPT_TEMPLATE_ID = "auto-assessment";

    private final AutoAssessmentConfig config;
    private final RetrievalService retrievalService;
    private final AiCallAuditService auditService;
    private final LlmClientSelector clientSelector;
    private final PromptTemplateLoader promptLoader;
    private final FindingRepository findingRepository;
    private final AiSuggestionRepository suggestionRepository;
    private final AiSourceRefRepository sourceRefRepository;
    private final AiCallAuditRepository auditRepository;

    public AutoAssessmentOrchestrator(
            AutoAssessmentConfig config,
            RetrievalService retrievalService,
            AiCallAuditService auditService,
            LlmClientSelector clientSelector,
            PromptTemplateLoader promptLoader,
            FindingRepository findingRepository,
            AiSuggestionRepository suggestionRepository,
            AiSourceRefRepository sourceRefRepository,
            AiCallAuditRepository auditRepository) {
        this.config = config;
        this.retrievalService = retrievalService;
        this.auditService = auditService;
        this.clientSelector = clientSelector;
        this.promptLoader = promptLoader;
        this.findingRepository = findingRepository;
        this.suggestionRepository = suggestionRepository;
        this.sourceRefRepository = sourceRefRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    @Transactional
    public Optional<CascadeOutcome> suggest(CascadeInput input) {
        if (!config.enabled()) {
            return Optional.empty();
        }
        RuleEvaluationContext ctx = input.evaluationContext();
        UUID findingId = ctx.finding().id();
        Finding finding = findingRepository.findById(findingId).orElse(null);
        if (finding == null) {
            log.debug("AutoAssessment: Finding {} nicht gefunden, skip.", findingId);
            return Optional.empty();
        }

        String advisory = baueAdvisoryAusschnitt(finding.getCve());
        List<RagHit> hits = retrievalService.similar(
                IndexingService.TYPE_ASSESSMENT,
                ctx.cve().cveId() + " " + ctx.component().name(),
                config.topK());

        AhsSeverity originalSeverity = mapeCvss(ctx.cve());
        Map<String, Object> vars = baueTemplateVariablen(ctx, finding, hits, advisory, originalSeverity);

        PromptTemplate template = promptLoader.load(PROMPT_TEMPLATE_ID);
        String userPrompt = template.renderUser(vars);
        String systemPrompt = template.renderSystem(Map.of());

        LlmRequest request = new LlmRequest(
                USE_CASE,
                template.id(),
                template.version(),
                systemPrompt,
                List.of(new Message(Message.Role.USER, userPrompt)),
                null,
                0.1,
                1024,
                input.environmentId(),
                "system:auto-assessment",
                hits.isEmpty() ? null : hits.stream()
                        .map(RagHit::chunkText)
                        .collect(Collectors.joining("\n---\n")),
                Map.of());

        LlmClient client;
        LlmResponse response;
        try {
            client = clientSelector.select(input.environmentId(), USE_CASE);
            response = auditService.execute(client, request);
        } catch (LlmDisabledException ex) {
            log.debug("AutoAssessment: LLM disabled.");
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("AutoAssessment fehlgeschlagen, faellt auf MANUAL: {}", ex.getMessage());
            return Optional.empty();
        }

        JsonNode out = response.structuredOutput();
        AhsSeverity proposed = parseSeverity(out, originalSeverity);
        String rationale = out.path("rationale").asText("");
        BigDecimal confidence = parseConfidence(out);
        List<String> usedFields = parseUsedProfileFields(out);
        String proposedFix = out.path("proposedFixVersion").isTextual()
                ? out.get("proposedFixVersion").asText() : null;

        boolean konservativerDefaultGreift =
                hits.stream().noneMatch(h -> h.score() >= config.minRagScore())
                        && (usedFields.isEmpty());
        if (konservativerDefaultGreift && proposed != originalSeverity) {
            log.debug("AutoAssessment: konservativer Default - Severity bleibt {}",
                    originalSeverity);
            proposed = originalSeverity;
            rationale = "Datenlage zu duenn fuer Downgrade. " + rationale;
        }

        boolean halluziniert = halluzinationsVerdacht(proposedFix, finding);
        AssessmentStatus targetStatus = halluziniert
                ? AssessmentStatus.NEEDS_VERIFICATION
                : AssessmentStatus.PROPOSED;

        AiSuggestion suggestion = persistiereSuggestion(
                response, finding, ctx, proposed, rationale, confidence, out);

        return Optional.of(CascadeOutcome.ai(
                suggestion.getId(),
                proposed,
                rationale,
                confidence,
                usedFields,
                targetStatus));
    }

    private AiSuggestion persistiereSuggestion(
            LlmResponse response,
            Finding finding,
            RuleEvaluationContext ctx,
            AhsSeverity severity,
            String rationale,
            BigDecimal confidence,
            JsonNode out) {
        // Den von AiCallAuditService geschriebenen PENDING/OK-Eintrag finden,
        // indem wir die zuletzt OK-finalisierte Zeile mit useCase und Prompt
        // verwenden. Pragmatisch: hier beschraenken wir uns auf die
        // Zuordnung ueber den letzten use-case-OK-Eintrag im selben Tx.
        AiCallAudit audit = auditRepository
                .findByStatusAndCreatedAtBefore(
                        com.ahs.cvm.domain.enums.AiCallStatus.OK,
                        java.time.Instant.now())
                .stream()
                .filter(a -> USE_CASE.equals(a.getUseCase()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException(
                        "Kein OK-Audit-Eintrag fuer Use-Case " + USE_CASE));

        AiSuggestion suggestion = AiSuggestion.builder()
                .aiCallAudit(audit)
                .useCase(USE_CASE)
                .finding(finding)
                .cve(finding.getCve())
                .severity(severity)
                .rationale(rationale)
                .confidence(confidence)
                .build();
        AiSuggestion persistiert = suggestionRepository.save(suggestion);

        if (out.has("sources") && out.get("sources").isArray()) {
            for (JsonNode src : out.get("sources")) {
                String kind = src.path("kind").asText("DOCUMENT");
                if (!List.of("PROFILE_PATH","CVE","RULE","DOCUMENT","CODE_REF").contains(kind)) {
                    kind = "DOCUMENT";
                }
                sourceRefRepository.save(AiSourceRef.builder()
                        .aiSuggestion(persistiert)
                        .kind(kind)
                        .reference(src.path("ref").asText(""))
                        .excerpt(src.path("excerpt").asText(""))
                        .build());
            }
        }
        return persistiert;
    }

    Map<String, Object> baueTemplateVariablen(
            RuleEvaluationContext ctx,
            Finding finding,
            List<RagHit> hits,
            String advisory,
            AhsSeverity originalSeverity) {
        CveSnapshot cve = ctx.cve();
        ComponentSnapshot comp = ctx.component();
        Map<String, Object> vars = new HashMap<>();
        vars.put("cveKey", cve.cveId());
        vars.put("originalSeverity", originalSeverity.name());
        vars.put("cvss", cve.cvssScore() == null ? "-" : cve.cvssScore().toPlainString());
        vars.put("kev", cve.kev() ? "ja" : "nein");
        vars.put("componentName", comp.name());
        vars.put("componentVersion", comp.version());
        vars.put("produktVersion", "-");
        vars.put("umgebung", "-");
        vars.put("profilAuszug",
                ctx.profile() == null ? "-" : ctx.profile().toString());
        vars.put("ragChunks", hits.isEmpty()
                ? "(keine RAG-Treffer)"
                : hits.stream()
                        .map(h -> "- " + h.chunkText().replaceAll("\\s+", " "))
                        .collect(Collectors.joining("\n")));
        vars.put("advisory", advisory);
        return vars;
    }

    static String baueAdvisoryAusschnitt(Cve cve) {
        if (cve == null) {
            return "(kein Advisory)";
        }
        StringBuilder sb = new StringBuilder();
        if (cve.getSummary() != null) {
            sb.append(cve.getSummary()).append('\n');
        }
        if (cve.getCwes() != null && !cve.getCwes().isEmpty()) {
            sb.append("CWE: ").append(String.join(",", cve.getCwes())).append('\n');
        }
        return sb.length() == 0 ? "(kein Advisory)" : sb.toString();
    }

    static AhsSeverity mapeCvss(CveSnapshot cve) {
        if (cve.cvssScore() == null) {
            return AhsSeverity.MEDIUM;
        }
        double score = cve.cvssScore().doubleValue();
        if (score >= 9.0) return AhsSeverity.CRITICAL;
        if (score >= 7.0) return AhsSeverity.HIGH;
        if (score >= 4.0) return AhsSeverity.MEDIUM;
        if (score > 0.0) return AhsSeverity.LOW;
        return AhsSeverity.INFORMATIONAL;
    }

    static AhsSeverity parseSeverity(JsonNode out, AhsSeverity fallback) {
        if (out == null || !out.has("severity") || !out.get("severity").isTextual()) {
            return fallback;
        }
        try {
            return AhsSeverity.valueOf(out.get("severity").asText());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    static BigDecimal parseConfidence(JsonNode out) {
        if (out == null || !out.has("confidence") || !out.get("confidence").isNumber()) {
            return BigDecimal.ZERO;
        }
        double v = out.get("confidence").asDouble();
        v = Math.max(0.0, Math.min(1.0, v));
        return BigDecimal.valueOf(v).setScale(3, java.math.RoundingMode.HALF_UP);
    }

    static List<String> parseUsedProfileFields(JsonNode out) {
        List<String> fields = new ArrayList<>();
        if (out != null && out.has("usedProfileFields") && out.get("usedProfileFields").isArray()) {
            for (JsonNode node : out.get("usedProfileFields")) {
                if (node.isTextual()) {
                    fields.add(node.asText());
                }
            }
        }
        return List.copyOf(fields);
    }

    static boolean halluzinationsVerdacht(String proposedFix, Finding finding) {
        if (proposedFix == null || proposedFix.isBlank()) {
            return false;
        }
        if (finding == null || finding.getFixedInVersion() == null) {
            // Modell behauptet eine Fix-Version, aber Advisory haelt keine
            // - das ist verdaechtig.
            return true;
        }
        return !proposedFix.trim().equalsIgnoreCase(finding.getFixedInVersion().trim());
    }

    /** ObjectMapper-Zugriff fuer Tests. */
    static ObjectMapper mapper() {
        return MAPPER;
    }
}
