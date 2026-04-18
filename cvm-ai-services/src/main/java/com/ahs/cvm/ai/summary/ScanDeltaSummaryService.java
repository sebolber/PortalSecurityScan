package com.ahs.cvm.ai.summary;

import com.ahs.cvm.llm.AiCallAuditService;
import com.ahs.cvm.llm.LlmClient;
import com.ahs.cvm.llm.LlmClient.LlmRequest;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClient.Message;
import com.ahs.cvm.llm.LlmClientSelector;
import com.ahs.cvm.llm.prompt.PromptTemplate;
import com.ahs.cvm.llm.prompt.PromptTemplateLoader;
import com.ahs.cvm.persistence.scan.Scan;
import com.ahs.cvm.persistence.scan.ScanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Erzeugt eine KI-Delta-Summary fuer einen Scan (Iteration 14, CVM-33).
 *
 * <p>Schritte:
 * <ol>
 *   <li>Letzten Scan derselben (ProduktVersion, Umgebung) finden.</li>
 *   <li>Strukturierten Diff via {@link ScanDeltaCalculator}.</li>
 *   <li>Wenn Diff &lt; {@code cvm.ai.summary.min-delta}: statischer
 *       Text ohne LLM-Call.</li>
 *   <li>Sonst: LLM-Call ueber {@link AiCallAuditService} mit Prompt
 *       {@code delta-summary}.</li>
 * </ol>
 */
@Service
public class ScanDeltaSummaryService {

    private static final Logger log = LoggerFactory.getLogger(ScanDeltaSummaryService.class);
    private static final String USE_CASE = "DELTA_SUMMARY";

    private final ScanRepository scanRepository;
    private final ScanDeltaCalculator calculator;
    private final AiCallAuditService auditService;
    private final LlmClientSelector clientSelector;
    private final PromptTemplateLoader templateLoader;
    private final int minDelta;

    public ScanDeltaSummaryService(
            ScanRepository scanRepository,
            ScanDeltaCalculator calculator,
            AiCallAuditService auditService,
            LlmClientSelector clientSelector,
            PromptTemplateLoader templateLoader,
            @Value("${cvm.ai.summary.min-delta:1}") int minDelta) {
        this.scanRepository = scanRepository;
        this.calculator = calculator;
        this.auditService = auditService;
        this.clientSelector = clientSelector;
        this.templateLoader = templateLoader;
        this.minDelta = Math.max(1, minDelta);
    }

    @Transactional
    public ScanDeltaSummary summarize(UUID scanId) {
        Scan current = scanRepository.findById(scanId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Scan nicht gefunden: " + scanId));
        Optional<Scan> previous = findePreviousScan(current);

        if (previous.isEmpty()) {
            ScanDelta delta = calculator.calculate(scanId, null);
            return new ScanDeltaSummary(scanId, null,
                    "Initial-Scan: " + delta.neueCves().size() + " CVEs gefunden.",
                    "Erster Scan dieser Produkt-Version in dieser Umgebung. "
                            + delta.neueCves().size()
                            + " CVEs sind im aktuellen SBOM enthalten.",
                    delta, false);
        }

        ScanDelta delta = calculator.calculate(scanId, previous.get().getId());
        if (delta.totalDelta() < minDelta) {
            return new ScanDeltaSummary(scanId, previous.get().getId(),
                    "Keine relevanten Aenderungen seit dem letzten Scan.",
                    "Der aktuelle Scan unterscheidet sich nicht (oder unter "
                            + "der konfigurierten Mindestschwelle) vom Vorgaenger.",
                    delta, false);
        }

        PromptTemplate template = templateLoader.load("delta-summary");
        Map<String, Object> vars = baueVars(current, delta);
        String userPrompt = template.renderUser(vars);
        String systemPrompt = template.renderSystem(Map.of());

        LlmClient client = clientSelector.select(
                current.getEnvironment() == null ? null
                        : current.getEnvironment().getId(),
                USE_CASE);
        LlmRequest request = new LlmRequest(
                USE_CASE,
                template.id(),
                template.version(),
                systemPrompt,
                List.of(new Message(Message.Role.USER, userPrompt)),
                null,
                0.2,
                1024,
                current.getEnvironment() == null ? null
                        : current.getEnvironment().getId(),
                "system:delta-summary",
                null,
                Map.of());
        LlmResponse response = auditService.execute(client, request);
        JsonNode out = response.structuredOutput();
        String shortText = out.path("short").asText("");
        String longText = out.path("long").asText("");
        if (shortText.isBlank()) {
            shortText = "Aenderungen seit letztem Scan: "
                    + delta.totalDelta() + " Eintraege.";
        }
        return new ScanDeltaSummary(scanId, previous.get().getId(),
                shortText, longText, delta, true);
    }

    private Optional<Scan> findePreviousScan(Scan current) {
        return scanRepository
                .findByProductVersionIdOrderByScannedAtDesc(
                        current.getProductVersion().getId())
                .stream()
                .filter(s -> !s.getId().equals(current.getId()))
                .filter(s -> sameEnvironment(s, current))
                .findFirst();
    }

    private static boolean sameEnvironment(Scan a, Scan b) {
        if (a.getEnvironment() == null || b.getEnvironment() == null) {
            return a.getEnvironment() == b.getEnvironment();
        }
        return a.getEnvironment().getId().equals(b.getEnvironment().getId());
    }

    private Map<String, Object> baueVars(Scan current, ScanDelta delta) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("produkt", current.getProductVersion().getProduct() == null
                ? "-" : current.getProductVersion().getProduct().getName());
        vars.put("produktVersion", current.getProductVersion().getVersion());
        vars.put("umgebung", current.getEnvironment() == null
                ? "-" : current.getEnvironment().getName());
        vars.put("neuCount", delta.neueCves().size());
        vars.put("neuListe", String.join(", ", delta.neueCves()));
        vars.put("entfallenCount", delta.entfalleneCves().size());
        vars.put("entfallenListe", String.join(", ", delta.entfalleneCves()));
        vars.put("shiftCount", delta.severityShifts().size());
        vars.put("shiftListe", delta.severityShifts().stream()
                .map(s -> s.cveKey() + " " + s.von() + "->" + s.nach())
                .reduce((a, b) -> a + ", " + b).orElse(""));
        vars.put("kevCount", delta.kevAenderungen().size());
        vars.put("kevListe", String.join(", ", delta.kevAenderungen()));
        return vars;
    }
}
