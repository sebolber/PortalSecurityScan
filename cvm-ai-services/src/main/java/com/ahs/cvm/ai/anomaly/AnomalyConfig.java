package com.ahs.cvm.ai.anomaly;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguration des Anomalie-Checks (Iteration 18, CVM-43).
 *
 * <p>Die {@code *Effective()}-Methoden lesen zur Laufzeit den System-
 * Parameter-Store mit Fallback auf die @Value-Defaults.
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
    private SystemParameterResolver resolver;

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

    @Autowired(required = false)
    public void setResolver(SystemParameterResolver resolver) {
        this.resolver = resolver;
    }

    public boolean enabled() { return enabled; }
    public double kevEpssThreshold() { return kevEpssThreshold; }
    public int manyAcceptRiskThreshold() { return manyAcceptRiskThreshold; }
    public double similarRejectionThreshold() { return similarRejectionThreshold; }
    public boolean useLlmSecondStage() { return useLlmSecondStage; }

    public boolean enabledEffective() {
        if (resolver == null) {
            return enabled;
        }
        return resolver.resolveBoolean("cvm.ai.anomaly.enabled", enabled);
    }

    public double kevEpssThresholdEffective() {
        if (resolver == null) {
            return kevEpssThreshold;
        }
        return resolver.resolveDouble("cvm.ai.anomaly.kev-epss-threshold", kevEpssThreshold);
    }

    public int manyAcceptRiskThresholdEffective() {
        if (resolver == null) {
            return manyAcceptRiskThreshold;
        }
        return Math.max(1, resolver.resolveInt(
                "cvm.ai.anomaly.many-accept-risk-threshold", manyAcceptRiskThreshold));
    }

    public double similarRejectionThresholdEffective() {
        if (resolver == null) {
            return similarRejectionThreshold;
        }
        return resolver.resolveDouble(
                "cvm.ai.anomaly.similar-rejection-threshold", similarRejectionThreshold);
    }

    public boolean useLlmSecondStageEffective() {
        if (resolver == null) {
            return useLlmSecondStage;
        }
        return resolver.resolveBoolean(
                "cvm.ai.anomaly.use-llm-second-stage", useLlmSecondStage);
    }
}
