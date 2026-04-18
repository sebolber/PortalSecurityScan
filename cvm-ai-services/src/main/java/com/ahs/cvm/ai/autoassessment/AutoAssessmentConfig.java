package com.ahs.cvm.ai.autoassessment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguration der KI-Vorbewertung (Iteration 13, CVM-32).
 *
 * <ul>
 *   <li>{@code cvm.ai.auto-assessment.enabled} (Default {@code false})
 *       schaltet die Cascade-Stufe&nbsp;3 ein.</li>
 *   <li>{@code cvm.ai.auto-assessment.top-k} (Default 5) - Anzahl
 *       RAG-Treffer fuer den Kontext.</li>
 *   <li>{@code cvm.ai.auto-assessment.min-rag-score} (Default 0.6)
 *       - unterhalb wird der konservative Default angewandt
 *       (Original-Severity, kein Downgrade).</li>
 * </ul>
 */
@Configuration
public class AutoAssessmentConfig {

    private final boolean enabled;
    private final int topK;
    private final double minRagScore;

    public AutoAssessmentConfig(
            @Value("${cvm.ai.auto-assessment.enabled:false}") boolean enabled,
            @Value("${cvm.ai.auto-assessment.top-k:5}") int topK,
            @Value("${cvm.ai.auto-assessment.min-rag-score:0.6}") double minRagScore) {
        this.enabled = enabled;
        this.topK = Math.max(1, topK);
        this.minRagScore = minRagScore;
    }

    public boolean enabled() {
        return enabled;
    }

    public int topK() {
        return topK;
    }

    public double minRagScore() {
        return minRagScore;
    }
}
