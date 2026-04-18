package com.ahs.cvm.llm;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguration des LLM-Gateways.
 *
 * <ul>
 *   <li>{@code cvm.llm.enabled} &mdash; Feature-Flag, Default {@code false}.</li>
 *   <li>{@code cvm.llm.injection.mode} &mdash; {@code warn} oder
 *       {@code block}.</li>
 *   <li>{@code cvm.llm.default-model} &mdash; Fallback-Modell, wenn
 *       kein Umgebungs-Profil zutrifft.</li>
 *   <li>{@code cvm.llm.rate-limit.global-per-minute} /
 *       {@code cvm.llm.rate-limit.tenant-per-minute} &mdash; Buckets.</li>
 * </ul>
 */
@Configuration
public class LlmGatewayConfig {

    private final boolean enabled;
    private final InjectionMode injectionMode;
    private final String defaultModel;
    private final int globalPerMinute;
    private final int tenantPerMinute;

    public LlmGatewayConfig(
            @Value("${cvm.llm.enabled:false}") boolean enabled,
            @Value("${cvm.llm.injection.mode:warn}") String injectionMode,
            @Value("${cvm.llm.default-model:claude-sonnet-4-6}") String defaultModel,
            @Value("${cvm.llm.rate-limit.global-per-minute:120}") int globalPerMinute,
            @Value("${cvm.llm.rate-limit.tenant-per-minute:30}") int tenantPerMinute) {
        this.enabled = enabled;
        this.injectionMode = InjectionMode.parse(injectionMode);
        this.defaultModel = defaultModel;
        this.globalPerMinute = Math.max(1, globalPerMinute);
        this.tenantPerMinute = Math.max(1, tenantPerMinute);
    }

    public boolean enabled() {
        return enabled;
    }

    public InjectionMode injectionMode() {
        return injectionMode;
    }

    public String defaultModel() {
        return defaultModel;
    }

    public int globalPerMinute() {
        return globalPerMinute;
    }

    public int tenantPerMinute() {
        return tenantPerMinute;
    }

    /**
     * Bequemer Zugriff auf die wichtigsten Flags fuer Tests.
     */
    public Map<String, Object> snapshot() {
        return Map.of(
                "enabled", enabled,
                "injectionMode", injectionMode.name(),
                "defaultModel", defaultModel,
                "globalPerMinute", globalPerMinute,
                "tenantPerMinute", tenantPerMinute);
    }

    public enum InjectionMode {
        WARN, BLOCK;

        static InjectionMode parse(String v) {
            if (v == null) {
                return WARN;
            }
            return switch (v.trim().toLowerCase()) {
                case "block" -> BLOCK;
                default -> WARN;
            };
        }
    }
}
