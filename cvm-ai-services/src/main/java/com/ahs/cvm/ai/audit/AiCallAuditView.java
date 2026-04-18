package com.ahs.cvm.ai.audit;

import com.ahs.cvm.domain.enums.AiCallStatus;
import com.ahs.cvm.persistence.ai.AiCallAudit;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-Model fuer den AI-Audit-Lese-Endpoint (Iteration 11 Nachzug).
 * System-/User-Prompts werden bewusst nicht ausgeliefert (PII-/Secret-
 * Risiko), nur Metadaten.
 */
public record AiCallAuditView(
        UUID id,
        String useCase,
        String modelId,
        AiCallStatus status,
        boolean injectionRisk,
        Integer promptTokens,
        Integer completionTokens,
        Integer latencyMs,
        BigDecimal costEur,
        String triggeredBy,
        UUID environmentId,
        Instant createdAt,
        Instant finalizedAt,
        String invalidOutputReason,
        String errorMessage) {

    public static AiCallAuditView from(AiCallAudit a) {
        return new AiCallAuditView(
                a.getId(),
                a.getUseCase(),
                a.getModelId(),
                a.getStatus(),
                Boolean.TRUE.equals(a.getInjectionRisk()),
                a.getPromptTokens(),
                a.getCompletionTokens(),
                a.getLatencyMs(),
                a.getCostEur(),
                a.getTriggeredBy(),
                a.getEnvironment() == null ? null : a.getEnvironment().getId(),
                a.getCreatedAt(),
                a.getFinalizedAt(),
                a.getInvalidOutputReason(),
                a.getErrorMessage());
    }
}
