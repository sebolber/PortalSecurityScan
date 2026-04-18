package com.ahs.cvm.ai.anomaly;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguration des Anomalie-Checks (Iteration 18, CVM-43).
 *
 * <ul>
 *   <li>{@code cvm.ai.anomaly.enabled} Default {@code false}.</li>
 *   <li>{@code cvm.ai.anomaly.kev-epss-threshold} Default 0.7 -
 *       EPSS-Schwelle fuer das KEV/NOT_APPLICABLE-Muster.</li>
 *   <li>{@code cvm.ai.anomaly.many-accept-risk-threshold}
 *       Default 5 - Waiver-Haeufung pro Bewerter in 24 h.</li>
 *   <li>{@code cvm.ai.anomaly.similar-rejection-threshold}
 *       Default 0.9 - String-Aehnlichkeit zur abgelehnten
 *       Begruendung.</li>
 *   <li>{@code cvm.ai.anomaly.use-llm-second-stage} Default
 *       {@code false} - LLM-Zweitpruefung nur bei explizitem
 *       Flag (haelt Kosten klein).</li>
 * </ul>
 */
@Configuration
public class AnomalyConfig {

    private final boolean enabled;
    private final double kevEpssThreshold;
    private final int manyAcceptRiskThreshold;
    private final double similarRejectionThreshold;
    private final boolean useLlmSecondStage;

    public AnomalyConfig(
            @Value("${cvm.ai.anomaly.enabled:false}") boolean enabled,
            @Value("${cvm.ai.anomaly.kev-epss-threshold:0.7}") double kevEpss,
            @Value("${cvm.ai.anomaly.many-accept-risk-threshold:5}") int waiver,
            @Value("${cvm.ai.anomaly.similar-rejection-threshold:0.9}") double similar,
            @Value("${cvm.ai.anomaly.use-llm-second-stage:false}") boolean useLlm) {
        this.enabled = enabled;
        this.kevEpssThreshold = kevEpss;
        this.manyAcceptRiskThreshold = Math.max(1, waiver);
        this.similarRejectionThreshold = similar;
        this.useLlmSecondStage = useLlm;
    }

    public boolean enabled() { return enabled; }
    public double kevEpssThreshold() { return kevEpssThreshold; }
    public int manyAcceptRiskThreshold() { return manyAcceptRiskThreshold; }
    public double similarRejectionThreshold() { return similarRejectionThreshold; }
    public boolean useLlmSecondStage() { return useLlmSecondStage; }
}
