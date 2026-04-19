package com.ahs.cvm.ai.reachability;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguration des Reachability-Agents (Iteration 15, CVM-40).
 *
 * <p>Die Legacy-Zugriffe {@link #enabled()}, {@link #timeout()} und
 * {@link #binary()} liefern die beim Boot aus {@code application.yaml}
 * gelesenen Werte. Die {@code *Effective()}-Varianten lesen zur Laufzeit
 * den System-Parameter-Store und fallen auf diese Defaults zurueck.
 *
 * <ul>
 *   <li>{@code cvm.ai.reachability.enabled} (Default {@code false}).</li>
 *   <li>{@code cvm.ai.reachability.timeout-seconds} (Default 300).</li>
 *   <li>{@code cvm.ai.reachability.binary} (Default {@code claude}).</li>
 * </ul>
 */
@Configuration
public class ReachabilityConfig {

    private final boolean enabled;
    private final Duration timeout;
    private final String binary;
    private final BigDecimal autoTriggerThreshold;
    private final int autoTriggerCooldownMinutes;
    private SystemParameterResolver resolver;

    @Autowired
    public ReachabilityConfig(
            @Value("${cvm.ai.reachability.enabled:false}") boolean enabled,
            @Value("${cvm.ai.reachability.timeout-seconds:300}") int timeoutSeconds,
            @Value("${cvm.ai.reachability.binary:claude}") String binary,
            @Value("${cvm.ai.reachability.auto-trigger-threshold:0.6}") BigDecimal autoTriggerThreshold,
            @Value("${cvm.ai.reachability.auto-trigger-cooldown-minutes:60}") int autoTriggerCooldownMinutes) {
        this.enabled = enabled;
        this.timeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
        this.binary = binary == null || binary.isBlank() ? "claude" : binary;
        this.autoTriggerThreshold = clamp01(autoTriggerThreshold == null
                ? new BigDecimal("0.6") : autoTriggerThreshold);
        this.autoTriggerCooldownMinutes = Math.max(0, autoTriggerCooldownMinutes);
    }

    /**
     * Bestands-Konstruktor aus Iteration 15. Bleibt fuer Bestands-
     * Unit-Tests erhalten, die die Iteration-70-Parameter
     * (auto-trigger-threshold/cooldown) nicht brauchen.
     */
    public ReachabilityConfig(boolean enabled, int timeoutSeconds, String binary) {
        this(enabled, timeoutSeconds, binary, new BigDecimal("0.6"), 60);
    }

    @Autowired(required = false)
    public void setResolver(SystemParameterResolver resolver) {
        this.resolver = resolver;
    }

    public boolean enabled() {
        return enabled;
    }

    public Duration timeout() {
        return timeout;
    }

    public String binary() {
        return binary;
    }

    public boolean enabledEffective() {
        if (resolver == null) {
            return enabled;
        }
        return resolver.resolveBoolean("cvm.ai.reachability.enabled", enabled);
    }

    public Duration timeoutEffective() {
        if (resolver == null) {
            return timeout;
        }
        int seconds = resolver.resolveInt("cvm.ai.reachability.timeout-seconds", (int) timeout.getSeconds());
        return Duration.ofSeconds(Math.max(5, seconds));
    }

    public String binaryEffective() {
        if (resolver == null) {
            return binary;
        }
        String value = resolver.resolveString("cvm.ai.reachability.binary", binary);
        return value == null || value.isBlank() ? "claude" : value;
    }

    /** Iteration 70 (CVM-307). */
    public BigDecimal autoTriggerThresholdEffective() {
        if (resolver == null) {
            return autoTriggerThreshold;
        }
        double v = resolver.resolveDouble(
                "cvm.ai.reachability.auto-trigger-threshold",
                autoTriggerThreshold.doubleValue());
        return clamp01(BigDecimal.valueOf(v));
    }

    /** Iteration 70 (CVM-307). */
    public int autoTriggerCooldownMinutesEffective() {
        if (resolver == null) {
            return autoTriggerCooldownMinutes;
        }
        return Math.max(0, resolver.resolveInt(
                "cvm.ai.reachability.auto-trigger-cooldown-minutes",
                autoTriggerCooldownMinutes));
    }

    private static BigDecimal clamp01(BigDecimal value) {
        if (value == null) {
            return new BigDecimal("0.6");
        }
        if (value.signum() < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return value;
    }
}
