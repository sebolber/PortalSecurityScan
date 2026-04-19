package com.ahs.cvm.ai.fixverify;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguration der Fix-Verifikation (Iteration 16, CVM-41).
 *
 * <p>Die {@code *Effective()}-Methoden lesen zur Laufzeit den System-
 * Parameter-Store und fallen auf die beim Boot aus {@code application.yaml}
 * gelesenen Defaults zurueck.
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
    private SystemParameterResolver resolver;

    public FixVerificationConfig(
            @Value("${cvm.ai.fix-verification.enabled:false}") boolean enabled,
            @Value("${cvm.ai.fix-verification.full-text-commit-cap:50}") int cap,
            @Value("${cvm.ai.fix-verification.cache-ttl-minutes:1440}") int ttl) {
        this.enabled = enabled;
        this.fullTextCommitCap = Math.max(5, cap);
        this.cacheTtlMinutes = Math.max(5, ttl);
    }

    @Autowired(required = false)
    public void setResolver(SystemParameterResolver resolver) {
        this.resolver = resolver;
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

    public boolean enabledEffective() {
        if (resolver == null) {
            return enabled;
        }
        return resolver.resolveBoolean("cvm.ai.fix-verification.enabled", enabled);
    }

    public int fullTextCommitCapEffective() {
        if (resolver == null) {
            return fullTextCommitCap;
        }
        return Math.max(5, resolver.resolveInt(
                "cvm.ai.fix-verification.full-text-commit-cap", fullTextCommitCap));
    }

    public int cacheTtlMinutesEffective() {
        if (resolver == null) {
            return cacheTtlMinutes;
        }
        return Math.max(5, resolver.resolveInt(
                "cvm.ai.fix-verification.cache-ttl-minutes", cacheTtlMinutes));
    }
}
