package com.ahs.cvm.llm.audit;

import com.ahs.cvm.domain.enums.AiCallStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistenz-Port fuer den LLM-Audit. Implementiert wird der Port
 * in einem Modul, das die Persistenz-Schicht kennt (typ.
 * {@code cvm-ai-services/JpaAiCallAuditAdapter}). Das LLM-Gateway
 * selbst haelt bewusst keine JPA-Abhaengigkeit.
 */
public interface AiCallAuditPort {

    /**
     * Legt einen neuen Audit-Eintrag im Status
     * {@link AiCallStatus#PENDING} an. Muss vor jedem LLM-Call
     * aufgerufen werden. Liefert die generierte Id.
     */
    UUID persistPending(AiCallAuditPending pending);

    /** Aktualisiert den bestehenden Eintrag mit dem Finalstatus. */
    void finalizeAudit(UUID auditId, AiCallAuditFinalization finalization);

    /** Eingabe fuer {@link #persistPending(AiCallAuditPending)}. */
    record AiCallAuditPending(
            String useCase,
            String modelId,
            String modelVersion,
            String promptTemplateId,
            String promptTemplateVersion,
            String systemPrompt,
            String userPrompt,
            String ragContext,
            String triggeredBy,
            UUID environmentId,
            boolean injectionRisk,
            Instant createdAt) {}

    /** Eingabe fuer {@link #finalizeAudit(UUID, AiCallAuditFinalization)}. */
    record AiCallAuditFinalization(
            AiCallStatus status,
            String rawResponse,
            Integer promptTokens,
            Integer completionTokens,
            Integer latencyMs,
            BigDecimal costEur,
            String invalidOutputReason,
            String errorMessage,
            Instant finalizedAt) {}
}
