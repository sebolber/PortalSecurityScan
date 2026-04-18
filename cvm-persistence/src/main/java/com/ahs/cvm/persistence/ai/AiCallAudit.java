package com.ahs.cvm.persistence.ai;

import com.ahs.cvm.domain.enums.AiCallStatus;
import com.ahs.cvm.persistence.environment.Environment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit-Eintrag fuer einen einzelnen LLM-Call (Iteration 11, CVM-30).
 *
 * <p>Der Datensatz wird zweistufig geschrieben: erst als
 * {@link AiCallStatus#PENDING} vor dem Call, danach im selben
 * Record-Zustand auf den Finalstatus ({@link AiCallStatus#OK} oder
 * Fehler) aktualisiert. Alle anderen Felder sind unveraenderlich; der
 * {@link AiCallAuditImmutabilityListener} setzt das durch.
 */
@Entity
@Table(name = "ai_call_audit")
@EntityListeners(AiCallAuditImmutabilityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AiCallAudit {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "use_case", nullable = false, updatable = false)
    private String useCase;

    @Column(name = "model_id", nullable = false, updatable = false)
    private String modelId;

    @Column(name = "model_version", updatable = false)
    private String modelVersion;

    @Column(name = "prompt_template_id", nullable = false, updatable = false)
    private String promptTemplateId;

    @Column(name = "prompt_template_version", nullable = false, updatable = false)
    private String promptTemplateVersion;

    @Column(name = "system_prompt", nullable = false, updatable = false, columnDefinition = "text")
    private String systemPrompt;

    @Column(name = "user_prompt", nullable = false, updatable = false, columnDefinition = "text")
    private String userPrompt;

    @Column(name = "rag_context", updatable = false, columnDefinition = "text")
    private String ragContext;

    @Column(name = "raw_response", columnDefinition = "text")
    private String rawResponse;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "cost_eur", precision = 10, scale = 6)
    private BigDecimal costEur;

    @Column(name = "triggered_by", nullable = false, updatable = false)
    private String triggeredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", updatable = false)
    private Environment environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AiCallStatus status;

    @Column(name = "injection_risk", nullable = false)
    private Boolean injectionRisk;

    @Column(name = "invalid_output_reason")
    private String invalidOutputReason;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    /**
     * Markiert die einzig erlaubte Finalisierung
     * (PENDING -&gt; OK/INVALID_OUTPUT/...). Der Listener prueft, dass
     * der Ausgangsstatus PENDING war.
     */
    @Transient
    @Builder.Default
    private boolean finalizingAllowed = false;

    @PrePersist
    void initialisiere() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = AiCallStatus.PENDING;
        }
        if (injectionRisk == null) {
            injectionRisk = Boolean.FALSE;
        }
    }
}
