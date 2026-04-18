package com.ahs.cvm.api.modelprofile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Eingabe-DTO fuer {@code POST /api/v1/llm-model-profiles}.
 *
 * <p>Der {@code provider} wird als String entgegengenommen und im
 * Application-Service in das {@code LlmModelProfile.Provider}-Enum
 * uebersetzt. So bleibt die API-Schicht frei von Persistence-Typen.
 *
 * <p>Bei {@code approvedForGkvData=true} ist {@code fourEyesConfirmer}
 * zwingend und muss ungleich {@code approvedBy} sein.
 */
public record ModelProfileCreateRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9_]{2,64}$",
                message = "profileKey muss ^[A-Z0-9_]{2,64}$ erfuellen")
        String profileKey,
        @NotBlank
        @Pattern(regexp = "CLAUDE_CLOUD|OLLAMA_ONPREM",
                message = "provider muss CLAUDE_CLOUD oder OLLAMA_ONPREM sein")
        String provider,
        @NotBlank
        @Size(max = 128)
        String modelId,
        @Size(max = 64)
        String modelVersion,
        @NotNull
        @DecimalMin(value = "0.0", message = "costBudgetEurMonthly muss >= 0 sein")
        BigDecimal costBudgetEurMonthly,
        boolean approvedForGkvData,
        @NotBlank
        @Size(max = 128)
        String approvedBy,
        @Size(max = 128)
        String fourEyesConfirmer,
        @Size(max = 500)
        String reason) {}
