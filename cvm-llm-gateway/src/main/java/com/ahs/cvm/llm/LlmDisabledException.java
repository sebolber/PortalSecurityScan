package com.ahs.cvm.llm;

/**
 * Wird geworfen, wenn das LLM-Feature per Flag deaktiviert ist
 * ({@code cvm.llm.enabled=false}). Vor jedem Call prueft das
 * {@link AiCallAuditService}, ob das Flag gesetzt ist; wenn nicht,
 * wird kein Call abgesetzt und die Exception fliegt.
 */
public class LlmDisabledException extends RuntimeException {

    public LlmDisabledException() {
        super("LLM-Feature ist deaktiviert (cvm.llm.enabled=false).");
    }

    public LlmDisabledException(String reason) {
        super("LLM-Feature ist deaktiviert: " + reason);
    }
}
