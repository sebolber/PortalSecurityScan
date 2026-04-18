package com.ahs.cvm.ai.ruleextraction;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguration der KI-Regel-Extraktion (Iteration 17, CVM-42).
 *
 * <ul>
 *   <li>{@code cvm.ai.rule-extraction.enabled} Default {@code false}.</li>
 *   <li>{@code cvm.ai.rule-extraction.window-days} Default 180.</li>
 *   <li>{@code cvm.ai.rule-extraction.cluster-cap} Default 10 - maximale
 *       Anzahl LLM-Calls pro Lauf.</li>
 *   <li>{@code cvm.ai.rule-extraction.override-review-threshold} Default
 *       4 - ab so vielen Overrides in 30 Tagen wird eine aktive AI-Regel
 *       auf DRAFT zurueckgesetzt.</li>
 * </ul>
 */
@Configuration
public class RuleExtractionConfig {

    private final boolean enabled;
    private final int windowDays;
    private final int clusterCap;
    private final int overrideReviewThreshold;

    public RuleExtractionConfig(
            @Value("${cvm.ai.rule-extraction.enabled:false}") boolean enabled,
            @Value("${cvm.ai.rule-extraction.window-days:180}") int windowDays,
            @Value("${cvm.ai.rule-extraction.cluster-cap:10}") int clusterCap,
            @Value("${cvm.ai.rule-extraction.override-review-threshold:4}")
                    int overrideReviewThreshold) {
        this.enabled = enabled;
        this.windowDays = Math.max(7, windowDays);
        this.clusterCap = Math.max(1, clusterCap);
        this.overrideReviewThreshold = Math.max(1, overrideReviewThreshold);
    }

    public boolean enabled() {
        return enabled;
    }

    public int windowDays() {
        return windowDays;
    }

    public int clusterCap() {
        return clusterCap;
    }

    public int overrideReviewThreshold() {
        return overrideReviewThreshold;
    }
}
