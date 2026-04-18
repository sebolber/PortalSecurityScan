package com.ahs.cvm.ai.copilot;

import com.ahs.cvm.ai.rag.IndexingService;
import com.ahs.cvm.ai.rag.RetrievalService;
import com.ahs.cvm.ai.rag.RetrievalService.RagHit;
import com.ahs.cvm.llm.AiCallAuditService;
import com.ahs.cvm.llm.LlmClient;
import com.ahs.cvm.llm.LlmClient.LlmRequest;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClient.Message;
import com.ahs.cvm.llm.LlmClientSelector;
import com.ahs.cvm.llm.prompt.PromptTemplate;
import com.ahs.cvm.llm.prompt.PromptTemplateLoader;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Stateless Copilot-Service (Iteration 14, CVM-33).
 *
 * <p>Jeder Call laeuft durch das LLM-Gateway und bekommt damit den
 * vollen Audit-/Injection-/Output-Validator-Pfad. Der Service
 * gibt nur den Vorschlagstext und Quellen zurueck - keinen Severity-
 * Wert, keinen Status. Das Prinzip "Mensch entscheidet" wird durch
 * {@link CopilotSuggestion} (kein Severity-Feld) und durch einen
 * Test in {@code CopilotServiceTest} hart geprueft.
 */
@Service
public class CopilotService {

    private static final String USE_CASE_BASE = "COPILOT";

    private final AssessmentRepository assessmentRepository;
    private final RetrievalService retrievalService;
    private final AiCallAuditService auditService;
    private final LlmClientSelector clientSelector;
    private final PromptTemplateLoader templateLoader;

    public CopilotService(
            AssessmentRepository assessmentRepository,
            RetrievalService retrievalService,
            AiCallAuditService auditService,
            LlmClientSelector clientSelector,
            PromptTemplateLoader templateLoader) {
        this.assessmentRepository = assessmentRepository;
        this.retrievalService = retrievalService;
        this.auditService = auditService;
        this.clientSelector = clientSelector;
        this.templateLoader = templateLoader;
    }

    public CopilotSuggestion suggest(CopilotRequest request) {
        Assessment assessment = assessmentRepository.findById(request.assessmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assessment nicht gefunden: " + request.assessmentId()));

        PromptTemplate template = templateLoader.load(request.useCase().templateId());
        Map<String, Object> vars = baueVars(assessment, request);
        String userPrompt = template.renderUser(vars);
        String systemPrompt = template.renderSystem(Map.of());

        LlmClient client = clientSelector.select(
                assessment.getEnvironment().getId(), useCaseLabel(request));
        LlmRequest llmRequest = new LlmRequest(
                useCaseLabel(request),
                template.id(),
                template.version(),
                systemPrompt,
                List.of(new Message(Message.Role.USER, userPrompt)),
                null,
                0.2,
                1024,
                assessment.getEnvironment().getId(),
                request.triggeredBy(),
                null,
                Map.of());
        LlmResponse response = auditService.execute(client, llmRequest);

        return mapResponse(request, response.structuredOutput());
    }

    Map<String, Object> baueVars(Assessment assessment, CopilotRequest request) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("cveKey", assessment.getCve().getCveId());
        vars.put("rationale", assessment.getRationale() == null
                ? "" : assessment.getRationale());
        vars.put("instruction", request.userInstruction());
        // Use-case-spezifisch
        switch (request.useCase()) {
            case SIMILAR_ASSESSMENTS -> {
                List<RagHit> hits = retrievalService.similar(
                        IndexingService.TYPE_ASSESSMENT,
                        assessment.getCve().getCveId(),
                        5);
                vars.put("ragChunks", hits.isEmpty()
                        ? "(keine Treffer)"
                        : hits.stream()
                                .map(h -> "- " + h.chunkText().replaceAll("\\s+", " "))
                                .collect(Collectors.joining("\n")));
            }
            case EXPLAIN_COMMIT -> vars.put("commit",
                    request.attachments().getOrDefault("commit", "(kein Commit)"));
            case REFINE_RATIONALE, AUDIT_TONE -> {
                // nur Standard-Variablen
            }
        }
        return vars;
    }

    static CopilotSuggestion mapResponse(CopilotRequest request, JsonNode out) {
        if (out == null || out.isMissingNode() || out.isNull()) {
            return new CopilotSuggestion(
                    request.assessmentId(), request.useCase(), "", List.of());
        }
        // Erstmal "text" lesen (Standard-Use-Cases). DeltaSummary
        // benutzt dieses Service nicht.
        String text = out.path("text").asText("");
        List<CopilotSuggestion.SourceRef> sources = new ArrayList<>();
        if (out.has("sources") && out.get("sources").isArray()) {
            for (JsonNode src : out.get("sources")) {
                sources.add(new CopilotSuggestion.SourceRef(
                        src.path("kind").asText("DOCUMENT"),
                        src.path("ref").asText(""),
                        src.path("excerpt").asText("")));
            }
        }
        return new CopilotSuggestion(
                request.assessmentId(), request.useCase(), text, sources);
    }

    private static String useCaseLabel(CopilotRequest request) {
        return USE_CASE_BASE + "_" + request.useCase().name();
    }
}
