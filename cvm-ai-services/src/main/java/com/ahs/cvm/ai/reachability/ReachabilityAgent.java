package com.ahs.cvm.ai.reachability;

import com.ahs.cvm.domain.enums.AiCallStatus;
import com.ahs.cvm.llm.audit.AiCallAuditPort;
import com.ahs.cvm.llm.audit.AiCallAuditPort.AiCallAuditFinalization;
import com.ahs.cvm.llm.audit.AiCallAuditPort.AiCallAuditPending;
import com.ahs.cvm.llm.prompt.PromptTemplate;
import com.ahs.cvm.llm.prompt.PromptTemplateLoader;
import com.ahs.cvm.llm.subprocess.SubprocessRunner;
import com.ahs.cvm.llm.subprocess.SubprocessRunner.SubprocessRequest;
import com.ahs.cvm.llm.subprocess.SubprocessRunner.SubprocessResult;
import com.ahs.cvm.persistence.ai.AiCallAudit;
import com.ahs.cvm.persistence.ai.AiCallAuditRepository;
import com.ahs.cvm.persistence.ai.AiSourceRef;
import com.ahs.cvm.persistence.ai.AiSourceRefRepository;
import com.ahs.cvm.persistence.ai.AiSuggestion;
import com.ahs.cvm.persistence.ai.AiSuggestionRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestriert die Reachability-Analyse via Subprocess (Iteration 15,
 * CVM-40).
 *
 * <p>Ablauf:
 * <ol>
 *   <li>Feature-Flag-Check.</li>
 *   <li>{@link GitCheckoutPort#checkout(String, String, String)}
 *       liefert das Arbeitsverzeichnis.</li>
 *   <li>Prompt-Datei in Temp-Verzeichnis schreiben.</li>
 *   <li>{@link SubprocessRunner} startet
 *       {@code claude code --read-only --prompt-file ... --output json}.</li>
 *   <li>JSON-Output parsen, validieren.</li>
 *   <li>Ergebnis als {@link AiSuggestion} mit
 *       use-case&nbsp;{@code REACHABILITY} und einem
 *       {@link AiSourceRef} pro Call-Site persistieren.</li>
 * </ol>
 */
@Service
public class ReachabilityAgent {

    private static final Logger log = LoggerFactory.getLogger(ReachabilityAgent.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ReachabilityConfig config;
    private final GitCheckoutPort gitCheckout;
    private final SubprocessRunner subprocess;
    private final PromptTemplateLoader templateLoader;
    private final FindingRepository findingRepository;
    private final AiSuggestionRepository suggestionRepository;
    private final AiSourceRefRepository sourceRefRepository;
    private final AiCallAuditPort auditPort;
    private final AiCallAuditRepository auditRepository;

    public ReachabilityAgent(
            ReachabilityConfig config,
            GitCheckoutPort gitCheckout,
            SubprocessRunner subprocess,
            PromptTemplateLoader templateLoader,
            FindingRepository findingRepository,
            AiSuggestionRepository suggestionRepository,
            AiSourceRefRepository sourceRefRepository,
            AiCallAuditPort auditPort,
            AiCallAuditRepository auditRepository) {
        this.config = config;
        this.gitCheckout = gitCheckout;
        this.subprocess = subprocess;
        this.templateLoader = templateLoader;
        this.findingRepository = findingRepository;
        this.suggestionRepository = suggestionRepository;
        this.sourceRefRepository = sourceRefRepository;
        this.auditPort = auditPort;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public ReachabilityResult analyze(ReachabilityRequest request) {
        if (!config.enabledEffective()) {
            return unavailable(request.findingId(), null,
                    "Reachability-Feature deaktiviert.");
        }
        Finding finding = findingRepository.findById(request.findingId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Finding nicht gefunden: " + request.findingId()));

        Path workdir = gitCheckout.checkout(
                request.repoUrl(), request.branch(), request.commitSha());
        Path promptFile = schreibePromptFile(workdir, request, finding);

        List<String> command = List.of(
                config.binaryEffective(), "code",
                "--read-only",
                "--prompt-file", promptFile.toAbsolutePath().toString(),
                "--output", "json");
        SubprocessRequest sub = new SubprocessRequest(
                command, workdir, promptFile, config.timeoutEffective(),
                Map.of("CVM_USECASE", "REACHABILITY"), true);

        // Audit PENDING vor Subprozess-Aufruf.
        UUID auditId = auditPort.persistPending(new AiCallAuditPending(
                "REACHABILITY",
                "claude-code-cli",
                null,
                "reachability",
                "v1",
                "subprocess --read-only --output json",
                "command=" + String.join(" ", command),
                null,
                request.triggeredBy(),
                null,
                false,
                Instant.now()));

        SubprocessResult res;
        try {
            res = subprocess.run(sub);
        } catch (RuntimeException ex) {
            log.warn("Subprocess-Fehler: {}", ex.getMessage());
            finalize(auditId, AiCallStatus.ERROR, "", null, null,
                    "Subprocess-Fehler: " + ex.getMessage());
            return unavailable(request.findingId(), auditId,
                    "Subprocess-Fehler: " + ex.getMessage());
        }
        if (res.timedOut()) {
            finalize(auditId, AiCallStatus.ERROR, res.stdout(), null,
                    (int) res.durationMs(), "timeout");
            return unavailable(request.findingId(), auditId,
                    "Reachability-Analyse Timeout.");
        }
        if (!res.ok()) {
            finalize(auditId, AiCallStatus.ERROR, res.stdout(), null,
                    (int) res.durationMs(),
                    "Subprocess Exit " + res.exitCode());
            return unavailable(request.findingId(), auditId,
                    "Reachability-Subprocess Exit " + res.exitCode());
        }

        JsonNode parsed;
        try {
            parsed = MAPPER.readTree(res.stdout());
        } catch (Exception ex) {
            finalize(auditId, AiCallStatus.INVALID_OUTPUT, res.stdout(), null,
                    (int) res.durationMs(),
                    "Output war kein JSON: " + ex.getMessage());
            return unavailable(request.findingId(), auditId,
                    "Output war kein JSON: " + ex.getMessage());
        }
        if (!parsed.has("findings") || !parsed.path("findings").has("callSites")
                || !parsed.path("findings").path("callSites").isArray()) {
            finalize(auditId, AiCallStatus.INVALID_OUTPUT, res.stdout(), null,
                    (int) res.durationMs(),
                    "Output verletzt Reachability-Schema.");
            return unavailable(request.findingId(), auditId,
                    "Output verletzt Reachability-Schema.");
        }

        List<ReachabilityResult.CallSite> sites = new ArrayList<>();
        for (JsonNode cs : parsed.path("findings").path("callSites")) {
            sites.add(new ReachabilityResult.CallSite(
                    cs.path("file").asText(""),
                    cs.path("line").asInt(0),
                    cs.path("symbol").asText(""),
                    cs.path("trust").asText("UNKNOWN"),
                    cs.path("note").asText("")));
        }
        String summary = parsed.path("summary").asText("");
        String recommendation = parsed.path("recommendation").asText("VERIFY");

        finalize(auditId, AiCallStatus.OK, res.stdout(), null,
                (int) res.durationMs(), null);

        AiCallAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalStateException(
                        "AiCallAudit nicht gefunden: " + auditId));
        AiSuggestion suggestion = suggestionRepository.save(AiSuggestion.builder()
                .aiCallAudit(audit)
                .useCase("REACHABILITY")
                .finding(finding)
                .cve(finding.getCve())
                .severity(null)
                .rationale("Reachability-Empfehlung: " + recommendation
                        + (summary == null || summary.isBlank()
                                ? "" : ". " + summary))
                .build());
        for (ReachabilityResult.CallSite site : sites) {
            sourceRefRepository.save(AiSourceRef.builder()
                    .aiSuggestion(suggestion)
                    .kind("CODE_REF")
                    .reference(site.file() + ":" + site.line() + " " + site.symbol())
                    .excerpt(site.trust() + " - " + site.note())
                    .build());
        }
        return new ReachabilityResult(
                request.findingId(), suggestion.getId(),
                recommendation, summary, sites, true, null);
    }

    private void finalize(UUID auditId, AiCallStatus status, String rawResponse,
            Integer completionTokens, Integer latencyMs, String errorMessage) {
        try {
            auditPort.finalize(auditId, new AiCallAuditFinalization(
                    status, rawResponse, null, completionTokens, latencyMs,
                    BigDecimal.ZERO, null, errorMessage, Instant.now()));
        } catch (RuntimeException ex) {
            log.warn("Audit-Finalize fehlgeschlagen: {}", ex.getMessage());
        }
    }

    private ReachabilityResult unavailable(UUID findingId, UUID auditId, String note) {
        log.info("Reachability nicht verfuegbar fuer {}: {}", findingId, note);
        return new ReachabilityResult(
                findingId, null, "VERIFY", note,
                List.of(), false, note);
    }

    private Path schreibePromptFile(
            Path workdir, ReachabilityRequest request, Finding finding) {
        PromptTemplate t = templateLoader.load("reachability");
        Map<String, Object> vars = new HashMap<>();
        vars.put("cveKey", finding.getCve().getCveId());
        vars.put("vulnerableSymbol", request.vulnerableSymbol());
        vars.put("language", request.language());
        vars.put("repoPath", workdir.toAbsolutePath().toString());
        vars.put("commit", request.commitSha() == null ? "HEAD" : request.commitSha());
        vars.put("instruction", request.instruction());
        String content = "# Reachability-Analyse\n\n## System\n"
                + t.renderSystem(Map.of()) + "\n\n## Auftrag\n"
                + t.renderUser(vars) + "\n";
        try {
            Path file = Files.createTempFile("cvm-reach-", ".md");
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return file;
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Prompt-Datei nicht schreibbar: " + ex.getMessage(), ex);
        }
    }
}
