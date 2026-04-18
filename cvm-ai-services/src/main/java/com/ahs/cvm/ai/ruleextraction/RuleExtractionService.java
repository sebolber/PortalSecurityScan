package com.ahs.cvm.ai.ruleextraction;

import com.ahs.cvm.ai.ruleextraction.AssessmentClusterer.AssessmentCluster;
import com.ahs.cvm.ai.ruleextraction.DryRunEvaluator.DryRunConflict;
import com.ahs.cvm.ai.ruleextraction.DryRunEvaluator.DryRunReport;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AiCallStatus;
import com.ahs.cvm.domain.enums.RuleSuggestionStatus;
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
import com.ahs.cvm.persistence.ai.AiSuggestion;
import com.ahs.cvm.persistence.ai.AiSuggestionRepository;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.rulesuggestion.RuleSuggestion;
import com.ahs.cvm.persistence.rulesuggestion.RuleSuggestionConflict;
import com.ahs.cvm.persistence.rulesuggestion.RuleSuggestionConflictRepository;
import com.ahs.cvm.persistence.rulesuggestion.RuleSuggestionExample;
import com.ahs.cvm.persistence.rulesuggestion.RuleSuggestionExampleRepository;
import com.ahs.cvm.persistence.rulesuggestion.RuleSuggestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestriert pro Cluster: LLM-Call -&gt; Dry-Run -&gt; Persistenz
 * (Iteration 17, CVM-42).
 */
@Service
public class RuleExtractionService {

    private static final Logger log = LoggerFactory.getLogger(RuleExtractionService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String USE_CASE = "RULE_EXTRACTION";
    private static final String TEMPLATE_ID = "rule-extraction";

    private final AiCallAuditService auditService;
    private final AiCallAuditRepository auditRepository;
    private final AiSuggestionRepository aiSuggestionRepository;
    private final LlmClientSelector clientSelector;
    private final PromptTemplateLoader templateLoader;
    private final DryRunEvaluator dryRun;
    private final RuleSuggestionRepository ruleSuggestionRepository;
    private final RuleSuggestionExampleRepository exampleRepository;
    private final RuleSuggestionConflictRepository conflictRepository;

    public RuleExtractionService(
            AiCallAuditService auditService,
            AiCallAuditRepository auditRepository,
            AiSuggestionRepository aiSuggestionRepository,
            LlmClientSelector clientSelector,
            PromptTemplateLoader templateLoader,
            DryRunEvaluator dryRun,
            RuleSuggestionRepository ruleSuggestionRepository,
            RuleSuggestionExampleRepository exampleRepository,
            RuleSuggestionConflictRepository conflictRepository) {
        this.auditService = auditService;
        this.auditRepository = auditRepository;
        this.aiSuggestionRepository = aiSuggestionRepository;
        this.clientSelector = clientSelector;
        this.templateLoader = templateLoader;
        this.dryRun = dryRun;
        this.ruleSuggestionRepository = ruleSuggestionRepository;
        this.exampleRepository = exampleRepository;
        this.conflictRepository = conflictRepository;
    }

    @Transactional
    public Optional<RuleSuggestion> extract(AssessmentCluster cluster, List<Assessment> historie) {
        if (cluster.assessments().size() < AssessmentClusterer.MIN_CLUSTER_SIZE
                || cluster.distinctCveKeys().size() < AssessmentClusterer.MIN_DISTINCT_CVES) {
            log.debug("Cluster unter Mindestgroesse, skip: {}", cluster.featureKey());
            return Optional.empty();
        }

        PromptTemplate template = templateLoader.load(TEMPLATE_ID);
        Map<String, Object> vars = buildTemplateVars(cluster);
        String userPrompt = template.renderUser(vars);
        String systemPrompt = template.renderSystem(Map.of());

        LlmClient client;
        LlmResponse response;
        try {
            client = clientSelector.select(null, USE_CASE);
            LlmRequest req = new LlmRequest(
                    USE_CASE, template.id(), template.version(),
                    systemPrompt,
                    List.of(new Message(Message.Role.USER, userPrompt)),
                    null, 0.1, 2048, null, "system:rule-extraction",
                    null, Map.of("clusterSize", cluster.assessments().size()));
            response = auditService.execute(client, req);
        } catch (RuntimeException ex) {
            log.warn("Rule-Extraktion LLM-Call fehlgeschlagen: {}", ex.getMessage());
            return Optional.empty();
        }

        JsonNode out = response.structuredOutput();
        JsonNode rule = out == null ? null : out.path("proposedRule");
        if (rule == null || rule.isMissingNode() || !rule.has("proposedSeverity")
                || !rule.has("condition")) {
            log.info("Rule-Extraktion liefert kein gueltiges Regel-Objekt: {}", cluster.featureKey());
            return Optional.empty();
        }

        AhsSeverity proposed;
        try {
            proposed = AhsSeverity.valueOf(rule.path("proposedSeverity").asText(""));
        } catch (IllegalArgumentException ex) {
            log.info("Rule-Extraktion: ungueltige Severity {}", rule.path("proposedSeverity"));
            return Optional.empty();
        }

        String conditionJson = rule.path("condition").toString();
        DryRunReport report = dryRun.evaluate(conditionJson, proposed, historie);

        AiCallAudit audit = auditRepository
                .findByStatusAndCreatedAtBefore(AiCallStatus.OK, Instant.now())
                .stream()
                .filter(a -> USE_CASE.equals(a.getUseCase()))
                .reduce((a, b) -> b)
                .orElseThrow(() -> new IllegalStateException(
                        "Kein OK-Audit fuer " + USE_CASE));

        AiSuggestion aiSuggestion = aiSuggestionRepository.save(AiSuggestion.builder()
                .aiCallAudit(audit)
                .useCase(USE_CASE)
                .severity(proposed)
                .rationale("Rule-Cluster " + cluster.featureKey()
                        + " (" + cluster.assessments().size() + " Assessments)")
                .build());

        RuleSuggestion suggestion = ruleSuggestionRepository.save(RuleSuggestion.builder()
                .aiSuggestion(aiSuggestion)
                .name(rule.path("name").asText("extracted-rule"))
                .conditionJson(conditionJson)
                .proposedSeverity(proposed)
                .rationaleTemplate(rule.path("rationaleTemplate").asText(""))
                .clusterRationale(out == null ? null
                        : out.path("clusterRationale").asText(""))
                .historicalMatchCount(report.historicalMatchCount())
                .wouldHaveCovered(report.wouldHaveCovered())
                .coverageRate(report.coverageRate())
                .conflictCount(report.conflictCount())
                .status(RuleSuggestionStatus.PROPOSED)
                .suggestedBy("system:rule-extraction")
                .build());

        for (Assessment a : cluster.assessments()) {
            exampleRepository.save(RuleSuggestionExample.builder()
                    .ruleSuggestion(suggestion)
                    .assessmentId(a.getId())
                    .build());
        }
        for (DryRunConflict c : report.conflicts()) {
            conflictRepository.save(RuleSuggestionConflict.builder()
                    .ruleSuggestion(suggestion)
                    .assessmentId(c.assessmentId())
                    .actualSeverity(c.actualSeverity())
                    .note(c.note())
                    .build());
        }
        log.info("Rule-Suggestion {} angelegt (Cluster {} Assessments, Coverage {})",
                suggestion.getId(), cluster.assessments().size(),
                report.coverageRate());
        return Optional.of(suggestion);
    }

    static Map<String, Object> buildTemplateVars(AssessmentCluster cluster) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("clusterSize", cluster.assessments().size());
        vars.put("cveCount", cluster.distinctCveKeys().size());
        vars.put("assessments", cluster.assessments().stream()
                .limit(8)
                .map(a -> "- " + safeCveKey(a) + " [" + a.getSeverity() + "] "
                        + shortRationale(a.getRationale()))
                .collect(Collectors.joining("\n")));
        vars.put("commonProfileFields",
                commonFields(cluster.assessments()));
        vars.put("rationaleSnippets", cluster.assessments().stream()
                .limit(5)
                .map(a -> shortRationale(a.getRationale()))
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(" | ")));
        return vars;
    }

    static String commonFields(List<Assessment> assessments) {
        Map<String, Integer> counts = new HashMap<>();
        for (Assessment a : assessments) {
            if (a.getRationaleSourceFields() == null) {
                continue;
            }
            Set<String> seen = new HashSet<>(a.getRationaleSourceFields());
            for (String f : seen) {
                counts.merge(f, 1, Integer::sum);
            }
        }
        int threshold = Math.max(2, assessments.size() / 2);
        List<String> common = new ArrayList<>();
        counts.forEach((k, v) -> {
            if (v >= threshold) {
                common.add(k);
            }
        });
        common.sort(String::compareTo);
        return common.isEmpty() ? "(keine)" : String.join(", ", common);
    }

    private static String safeCveKey(Assessment a) {
        return a.getCve() == null ? "(CVE?)" : a.getCve().getCveId();
    }

    private static String shortRationale(String rationale) {
        if (rationale == null) {
            return "";
        }
        return rationale.length() > 160
                ? rationale.substring(0, 160) + "..."
                : rationale;
    }
}
