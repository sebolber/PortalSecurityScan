package com.ahs.cvm.ai.fixverify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguration der Fix-Verifikation (Iteration 16, CVM-41).
 *
 * <ul>
 *   <li>{@code cvm.ai.fix-verification.enabled} Default {@code false}.</li>
 *   <li>{@code cvm.ai.fix-verification.full-text-commit-cap} Default 50:
 *       ab hier fliessen nur noch Messages in den Prompt.</li>
 *   <li>{@code cvm.ai.fix-verification.cache-ttl-minutes} Default 1440
 *       (24 h).</li>
 * </ul>
 */
@Configuration
public class FixVerificationConfig {

    private final boolean enabled;
    private final int fullTextCommitCap;
    private final int cacheTtlMinutes;

    public FixVerificationConfig(
            @Value("${cvm.ai.fix-verification.enabled:false}") boolean enabled,
            @Value("${cvm.ai.fix-verification.full-text-commit-cap:50}") int cap,
            @Value("${cvm.ai.fix-verification.cache-ttl-minutes:1440}") int ttl) {
        this.enabled = enabled;
        this.fullTextCommitCap = Math.max(5, cap);
        this.cacheTtlMinutes = Math.max(5, ttl);
    }

    public boolean enabled() {
        return enabled;
    }

    public int fullTextCommitCap() {
        return fullTextCommitCap;
    }

    public int cacheTtlMinutes() {
        return cacheTtlMinutes;
    }
}
