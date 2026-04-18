package com.ahs.cvm.ai.reachability;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguration des Reachability-Agents (Iteration 15, CVM-40).
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

    public ReachabilityConfig(
            @Value("${cvm.ai.reachability.enabled:false}") boolean enabled,
            @Value("${cvm.ai.reachability.timeout-seconds:300}") int timeoutSeconds,
            @Value("${cvm.ai.reachability.binary:claude}") String binary) {
        this.enabled = enabled;
        this.timeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
        this.binary = binary == null || binary.isBlank() ? "claude" : binary;
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
}
