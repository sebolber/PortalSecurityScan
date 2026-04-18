package com.ahs.cvm.ai.nlquery;

import com.ahs.cvm.ai.nlquery.NlFilter.SortBy;
import com.ahs.cvm.ai.nlquery.NlFilterValidator.ValidationResult;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NL-Query-Service (Iteration 19, CVM-50). Der Service erzeugt
 * selbst KEIN SQL und liest auch keines aus der LLM-Antwort. Er
 * konsumiert ausschliesslich den validierten {@link NlFilter} und
 * baut die Ergebnisliste ueber {@link AssessmentRepository#findAll}
 * mit Stream-Filtering. Das ist fuer den Fachumfang ausreichend und
 * haelt die Invariante "kein freies SQL vom LLM" trivial erfuellt.
 */
@Service
public class NlQueryService {

    private static final Logger log = LoggerFactory.getLogger(NlQueryService.class);
    private static final String USE_CASE = "NL_QUERY";
    private static final String TEMPLATE_ID = "nl-to-filter";

    private final AiCallAuditService auditService;
    private final LlmClientSelector clientSelector;
    private final PromptTemplateLoader templateLoader;
    private final NlFilterValidator validator;
    private final AssessmentRepository assessmentRepository;
    private final int resultLimit;

    public NlQueryService(
            AiCallAuditService auditService,
            LlmClientSelector clientSelector,
            PromptTemplateLoader templateLoader,
            NlFilterValidator validator,
            AssessmentRepository assessmentRepository,
            @Value("${cvm.ai.nl-query.result-limit:100}") int resultLimit) {
        this.auditService = auditService;
        this.clientSelector = clientSelector;
        this.templateLoader = templateLoader;
        this.validator = validator;
        this.assessmentRepository = assessmentRepository;
        this.resultLimit = Math.max(10, resultLimit);
    }

    @Transactional(readOnly = true)
    public NlQueryResult query(String nlQuestion, String triggeredBy) {
        if (nlQuestion == null || nlQuestion.isBlank()) {
            throw new IllegalArgumentException("Frage darf nicht leer sein.");
        }
        if (triggeredBy == null || triggeredBy.isBlank()) {
            throw new IllegalArgumentException("triggeredBy darf nicht leer sein.");
        }

        PromptTemplate template = templateLoader.load(TEMPLATE_ID);
        Map<String, Object> vars = new HashMap<>();
        vars.put("question", nlQuestion);
        vars.put("now", Instant.now().toString());
        String systemPrompt = template.renderSystem(Map.of());
        String userPrompt = template.renderUser(vars);

        LlmClient client = clientSelector.select(null, USE_CASE);
        LlmRequest req = new LlmRequest(
                USE_CASE, template.id(), template.version(),
                systemPrompt,
                List.of(new Message(Message.Role.USER, userPrompt)),
                null, 0.0, 1024, null, triggeredBy,
                null, Map.of());

        LlmResponse response;
        try {
            response = auditService.execute(client, req);
        } catch (RuntimeException ex) {
            log.warn("NL-Query-LLM fehlgeschlagen: {}", ex.getMessage());
            return new NlQueryResult(null, "",
                    List.of(),
                    List.of("LLM-Call fehlgeschlagen: " + ex.getMessage()));
        }

        ValidationResult validation = validator.validate(response.structuredOutput());
        if (!validation.ok()) {
            log.info("NL-Query abgelehnt: {}", validation.errors());
            return new NlQueryResult(null, validation.explanation(),
                    List.of(), validation.errors());
        }

        List<NlQueryResult.Row> rows = runFilter(validation.filter());
        return new NlQueryResult(validation.filter(),
                validation.explanation(), rows, List.of());
    }

    List<NlQueryResult.Row> runFilter(NlFilter filter) {
        Instant now = Instant.now();
        return assessmentRepository.findAll().stream()
                .filter(a -> a.getSupersededAt() == null)
                .filter(a -> matchEnv(a, filter.environmentKey()))
                .filter(a -> matchPv(a, filter.productVersionLabel()))
                .filter(a -> filter.severityIn().isEmpty()
                        || filter.severityIn().contains(a.getSeverity()))
                .filter(a -> filter.statusIn().isEmpty()
                        || filter.statusIn().contains(a.getStatus()))
                .filter(a -> filter.minAgeDays() == null
                        || isOlderThan(a, filter.minAgeDays(), now))
                .filter(a -> filter.hasUpstreamFix() == null
                        || hasFix(a) == filter.hasUpstreamFix())
                .filter(a -> filter.kevOnly() == null
                        || !filter.kevOnly()
                        || (a.getCve() != null
                            && Boolean.TRUE.equals(a.getCve().getKevListed())))
                .sorted(compareBy(filter.sortBy()))
                .limit(resultLimit)
                .map(a -> new NlQueryResult.Row(
                        a.getId(),
                        a.getCve() == null ? null : a.getCve().getCveId(),
                        a.getSeverity(),
                        a.getStatus(),
                        a.getEnvironment() == null ? null : a.getEnvironment().getKey(),
                        a.getCreatedAt()))
                .toList();
    }

    private static boolean matchEnv(Assessment a, String envKey) {
        if (envKey == null) {
            return true;
        }
        return a.getEnvironment() != null
                && Objects.equals(a.getEnvironment().getKey(), envKey);
    }

    private static boolean matchPv(Assessment a, String label) {
        if (label == null) {
            return true;
        }
        if (a.getProductVersion() == null) {
            return false;
        }
        String version = a.getProductVersion().getVersion();
        String productName = a.getProductVersion().getProduct() == null
                ? "" : a.getProductVersion().getProduct().getName();
        return label.equalsIgnoreCase(version)
                || (productName + " " + version).equalsIgnoreCase(label);
    }

    private static boolean isOlderThan(Assessment a, int minDays, Instant now) {
        if (a.getCreatedAt() == null) {
            return false;
        }
        return Duration.between(a.getCreatedAt(), now).toDays() >= minDays;
    }

    private static boolean hasFix(Assessment a) {
        return a.getFinding() != null
                && a.getFinding().getFixedInVersion() != null
                && !a.getFinding().getFixedInVersion().isBlank();
    }

    private static Comparator<Assessment> compareBy(SortBy sortBy) {
        Comparator<Assessment> c = Comparator.comparing(
                Assessment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        return switch (sortBy) {
            case AGE_DESC -> c.reversed();
            case AGE_ASC -> c;
            case SEVERITY_DESC -> Comparator.comparingInt(
                    (Assessment a) -> a.getSeverity() == null ? 99 : a.getSeverity().ordinal());
            case SEVERITY_ASC -> Comparator.comparingInt(
                    (Assessment a) -> a.getSeverity() == null ? 99 : a.getSeverity().ordinal())
                    .reversed();
        };
    }
}
