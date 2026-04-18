package com.ahs.cvm.ai.profileassistant;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguration des Profil-Assistenten (Iteration 18, CVM-43).
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

    public ProfileAssistantConfig(
            @Value("${cvm.ai.profile-assistant.enabled:false}") boolean enabled,
            @Value("${cvm.ai.profile-assistant.session-ttl-hours:24}") int ttlHours) {
        this.enabled = enabled;
        this.sessionTtl = Duration.ofHours(Math.max(1, ttlHours));
    }

    public boolean enabled() { return enabled; }
    public Duration sessionTtl() { return sessionTtl; }
}
