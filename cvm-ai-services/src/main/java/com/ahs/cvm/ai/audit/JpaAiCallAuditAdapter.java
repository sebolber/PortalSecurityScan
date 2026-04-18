package com.ahs.cvm.ai.audit;

import com.ahs.cvm.domain.enums.AiCallStatus;
import com.ahs.cvm.llm.audit.AiCallAuditPort;
import com.ahs.cvm.persistence.ai.AiCallAudit;
import com.ahs.cvm.persistence.ai.AiCallAuditRepository;
import com.ahs.cvm.persistence.environment.Environment;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-Implementation des {@link AiCallAuditPort}. Lebt im
 * {@code cvm-ai-services}-Modul, damit das {@code cvm-llm-gateway} frei
 * von Persistenz-Abhaengigkeiten bleibt (ArchUnit-Regel in CLAUDE.md
 * Abschnitt 3).
 */
@Component
public class JpaAiCallAuditAdapter implements AiCallAuditPort {

    private final AiCallAuditRepository repository;

    public JpaAiCallAuditAdapter(AiCallAuditRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public UUID persistPending(AiCallAuditPending pending) {
        AiCallAudit audit = AiCallAudit.builder()
                .useCase(pending.useCase())
                .modelId(pending.modelId())
                .modelVersion(pending.modelVersion())
                .promptTemplateId(pending.promptTemplateId())
                .promptTemplateVersion(pending.promptTemplateVersion())
                .systemPrompt(pending.systemPrompt())
                .userPrompt(pending.userPrompt())
                .ragContext(pending.ragContext())
                .triggeredBy(pending.triggeredBy())
                .environment(pending.environmentId() == null ? null
                        : Environment.builder().id(pending.environmentId()).build())
                .injectionRisk(pending.injectionRisk())
                .status(AiCallStatus.PENDING)
                .createdAt(pending.createdAt())
                .build();
        return repository.save(audit).getId();
    }

    @Override
    @Transactional
    public void finalize(UUID auditId, AiCallAuditFinalization finalization) {
        AiCallAudit audit = repository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Audit-Eintrag nicht gefunden: " + auditId));
        if (audit.getStatus() != AiCallStatus.PENDING) {
            throw new IllegalStateException(
                    "Audit %s ist nicht mehr PENDING: %s"
                            .formatted(auditId, audit.getStatus()));
        }
        audit.setFinalizingAllowed(true);
        audit.setStatus(finalization.status());
        audit.setRawResponse(finalization.rawResponse());
        audit.setPromptTokens(finalization.promptTokens());
        audit.setCompletionTokens(finalization.completionTokens());
        audit.setLatencyMs(finalization.latencyMs());
        audit.setCostEur(finalization.costEur());
        audit.setInvalidOutputReason(finalization.invalidOutputReason());
        audit.setErrorMessage(finalization.errorMessage());
        audit.setFinalizedAt(finalization.finalizedAt());
        repository.save(audit);
    }
}
