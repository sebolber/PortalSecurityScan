package com.ahs.cvm.llm;

import com.ahs.cvm.domain.enums.AiCallStatus;
import com.ahs.cvm.llm.LlmClient.LlmRequest;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmGatewayConfig.InjectionMode;
import com.ahs.cvm.llm.audit.AiCallAuditPort;
import com.ahs.cvm.llm.audit.AiCallAuditPort.AiCallAuditFinalization;
import com.ahs.cvm.llm.audit.AiCallAuditPort.AiCallAuditPending;
import com.ahs.cvm.llm.budget.CostBudgetPort;
import com.ahs.cvm.llm.cost.LlmCostCalculator;
import com.ahs.cvm.llm.injection.InjectionDetector;
import com.ahs.cvm.llm.injection.InjectionDetector.InjectionVerdict;
import com.ahs.cvm.llm.rate.LlmRateLimiter;
import com.ahs.cvm.llm.validate.OutputValidator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Verpflichtender Wrapper um jeden LLM-Call. Bildet die
 * Sicherheits-Invarianten ab, die in
 * {@code docs/konzept/llm-gateway-invarianten.md} aufgelistet sind.
 *
 * <p>Call-Ablauf:
 * <ol>
 *   <li>Feature-Flag-Check ({@link LlmDisabledException}).</li>
 *   <li>Rate-Limit pro Mandant und global.</li>
 *   <li>PENDING-Audit-Eintrag persistieren.</li>
 *   <li>Injection-Detektor auf User-Prompt + RAG-Kontext.</li>
 *   <li>LLM-Call (falls nicht geblockt).</li>
 *   <li>Output-Validator auf strukturierte Antwort.</li>
 *   <li>Audit finalisieren (OK oder Fehlerstatus).</li>
 * </ol>
 * Jeder frueh-Abbruch finalisiert das Audit mit passendem Status.
 */
@Service
public class AiCallAuditService {

    private static final Logger log = LoggerFactory.getLogger(AiCallAuditService.class);

    private final LlmGatewayConfig config;
    private final AiCallAuditPort auditPort;
    private final InjectionDetector injectionDetector;
    private final OutputValidator outputValidator;
    private final LlmRateLimiter rateLimiter;
    private final LlmCostCalculator costCalculator;
    private final CostBudgetPort costBudget;
    private final Clock clock;

    public AiCallAuditService(
            LlmGatewayConfig config,
            AiCallAuditPort auditPort,
            InjectionDetector injectionDetector,
            OutputValidator outputValidator,
            LlmRateLimiter rateLimiter,
            LlmCostCalculator costCalculator,
            CostBudgetPort costBudget,
            Clock clock) {
        this.config = config;
        this.auditPort = auditPort;
        this.injectionDetector = injectionDetector;
        this.outputValidator = outputValidator;
        this.rateLimiter = rateLimiter;
        this.costCalculator = costCalculator;
        this.costBudget = costBudget;
        this.clock = clock;
    }

    /**
     * Fuehrt den Call durch den gesamten Audit-Pfad. Der uebergebene
     * {@link LlmClient} wird nur nach allen Sicherheits-Checks
     * aufgerufen.
     */
    public LlmResponse execute(LlmClient client, LlmRequest request) {
        if (!config.enabled()) {
            log.info("LLM-Call abgelehnt: Feature-Flag aus (useCase={}).", request.useCase());
            throw new LlmDisabledException();
        }

        if (!costBudget.isUnderBudget(request.environmentId())) {
            UUID auditId = auditPort.persistPending(pending(request, client.modelId(), false));
            auditPort.finalizeAudit(auditId, finalization(
                    AiCallStatus.DISABLED, null, null, null, null, BigDecimal.ZERO,
                    null, "Monatsbudget fuer LLM-Profil aufgebraucht"));
            log.warn("LLM-Call abgelehnt: Monatsbudget aufgebraucht (useCase={}, env={}).",
                    request.useCase(), request.environmentId());
            throw new LlmDisabledException(
                    "Monatsbudget fuer LLM-Profil aufgebraucht");
        }

        String tenant = tenantOf(request);
        if (!rateLimiter.tryAcquire(tenant)) {
            UUID auditId = auditPort.persistPending(pending(request, client.modelId(), false));
            auditPort.finalizeAudit(auditId, finalization(
                    AiCallStatus.RATE_LIMITED, null, null, null, null, BigDecimal.ZERO,
                    null, "Rate-Limit ueberschritten"));
            throw new LlmRateLimitException(tenant);
        }

        InjectionVerdict verdict = injectionDetector.checkAll(
                joinMessages(request), request.ragContext());
        boolean injectionRisk = verdict.suspicious();

        UUID auditId = auditPort.persistPending(
                pending(request, client.modelId(), injectionRisk));

        if (injectionRisk && config.injectionMode() == InjectionMode.BLOCK) {
            log.warn("LLM-Call blockiert (Injection-Risiko): marker={}", verdict.marker());
            auditPort.finalizeAudit(auditId, finalization(
                    AiCallStatus.INJECTION_RISK, null, null, null, null, BigDecimal.ZERO,
                    null, "Injection-Marker: " + String.join(", ", verdict.marker())));
            throw new InjectionRiskException(verdict.marker());
        }

        Instant start = Instant.now(clock);
        LlmResponse response;
        try {
            response = client.complete(request);
        } catch (RuntimeException ex) {
            log.warn("LLM-Call fehlgeschlagen: useCase={}, fehler={}",
                    request.useCase(), ex.getMessage());
            auditPort.finalizeAudit(auditId, finalization(
                    AiCallStatus.ERROR, null, null, null,
                    (int) Duration.between(start, Instant.now(clock)).toMillis(),
                    BigDecimal.ZERO, null, ex.getClass().getSimpleName() + ": " + ex.getMessage()));
            throw ex;
        }

        List<String> validationErrors = outputValidator.validate(
                response.structuredOutput(), request.outputSchema());
        int latencyMs = (int) response.latency().toMillis();
        BigDecimal cost = costCalculator.calculate(response.modelId(), response.usage());

        if (!validationErrors.isEmpty()) {
            String grund = String.join("; ", validationErrors);
            log.warn("LLM-Output verworfen: {}", grund);
            auditPort.finalizeAudit(auditId, finalization(
                    AiCallStatus.INVALID_OUTPUT, response.rawText(),
                    tokens(response, true), tokens(response, false),
                    latencyMs, cost, grund, null));
            throw new InvalidLlmOutputException(grund);
        }

        auditPort.finalizeAudit(auditId, finalization(
                AiCallStatus.OK, response.rawText(),
                tokens(response, true), tokens(response, false),
                latencyMs, cost, null, null));
        if (injectionRisk) {
            log.info("LLM-Call OK trotz Injection-Risiko (Warn-Modus): marker={}",
                    verdict.marker());
        }
        return response;
    }

    private AiCallAuditPending pending(LlmRequest req, String modelId, boolean injectionRisk) {
        return new AiCallAuditPending(
                req.useCase(),
                modelId,
                null,
                req.promptTemplateId(),
                req.promptTemplateVersion(),
                req.systemPrompt(),
                joinMessages(req),
                req.ragContext(),
                req.triggeredBy(),
                req.environmentId(),
                injectionRisk,
                Instant.now(clock));
    }

    private AiCallAuditFinalization finalization(
            AiCallStatus status, String rawResponse,
            Integer promptTokens, Integer completionTokens,
            Integer latencyMs, BigDecimal cost,
            String invalidReason, String errorMessage) {
        return new AiCallAuditFinalization(
                status,
                rawResponse,
                promptTokens,
                completionTokens,
                latencyMs,
                cost == null ? BigDecimal.ZERO : cost,
                invalidReason,
                errorMessage,
                Instant.now(clock));
    }

    private static String joinMessages(LlmRequest request) {
        StringBuilder sb = new StringBuilder();
        for (LlmClient.Message m : request.messages()) {
            sb.append('[').append(m.role().name()).append("] ")
                    .append(m.content()).append('\n');
        }
        return sb.toString();
    }

    private static Integer tokens(LlmResponse response, boolean prompt) {
        if (response.usage() == null) {
            return null;
        }
        return prompt ? response.usage().promptTokens() : response.usage().completionTokens();
    }

    private static String tenantOf(LlmRequest request) {
        Object tenant = request.metadata().get("tenant");
        return tenant == null ? null : tenant.toString();
    }

    /** Wird geworfen, wenn der Rate-Limiter verweigert. */
    public static class LlmRateLimitException extends RuntimeException {
        public LlmRateLimitException(String tenant) {
            super("Rate-Limit ueberschritten"
                    + (tenant == null ? "" : " (tenant=" + tenant + ")"));
        }
    }
}
