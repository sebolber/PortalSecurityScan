package com.ahs.cvm.ai.profileassistant;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguration des Profil-Assistenten (Iteration 18, CVM-43).
 *
 * <p>{@code *Effective()}-Varianten lesen zur Laufzeit den System-
 * Parameter-Store und fallen auf die Boot-Werte zurueck, wenn der
 * Parameter nicht gesetzt ist (Iteration 61, CVM-62).
 *
 * <ul>
 *   <li>{@code cvm.ai.profile-assistant.enabled} Default {@code false}.</li>
 *   <li>{@code cvm.ai.profile-assistant.session-ttl-hours} Default 24.</li>
 * </ul>
 */
@Configuration
public class ProfileAssistantConfig {

    private final boolean enabled;
    private final Duration sessionTtl;
    private SystemParameterResolver resolver;

    public ProfileAssistantConfig(
            @Value("${cvm.ai.profile-assistant.enabled:false}") boolean enabled,
            @Value("${cvm.ai.profile-assistant.session-ttl-hours:24}") int ttlHours) {
        this.enabled = enabled;
        this.sessionTtl = Duration.ofHours(Math.max(1, ttlHours));
    }

    @Autowired(required = false)
    public void setResolver(SystemParameterResolver resolver) {
        this.resolver = resolver;
    }

    public boolean enabled() { return enabled; }
    public Duration sessionTtl() { return sessionTtl; }

    public boolean enabledEffective() {
        if (resolver == null) {
            return enabled;
        }
        return resolver.resolveBoolean("cvm.ai.profile-assistant.enabled", enabled);
    }

    public Duration sessionTtlEffective() {
        if (resolver == null) {
            return sessionTtl;
        }
        int hours = resolver.resolveInt(
                "cvm.ai.profile-assistant.session-ttl-hours",
                (int) sessionTtl.toHours());
        return Duration.ofHours(Math.max(1, hours));
    }
}
