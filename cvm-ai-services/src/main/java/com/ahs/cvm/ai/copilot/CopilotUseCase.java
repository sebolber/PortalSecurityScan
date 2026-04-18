package com.ahs.cvm.ai.copilot;

/**
 * Bekannte Copilot-Use-Cases (Iteration 14, CVM-33). Jeder Wert ist
 * an einen festen Prompt-Template-Namen gebunden.
 */
public enum CopilotUseCase {
    REFINE_RATIONALE("refine-rationale"),
    SIMILAR_ASSESSMENTS("similar-assessments"),
    EXPLAIN_COMMIT("explain-commit"),
    AUDIT_TONE("audit-tone");

    private final String templateId;

    CopilotUseCase(String templateId) {
        this.templateId = templateId;
    }

    public String templateId() {
        return templateId;
    }
}
