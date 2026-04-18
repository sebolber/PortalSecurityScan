package com.ahs.cvm.application.environment;

import com.ahs.cvm.domain.enums.EnvironmentStage;
import java.util.UUID;

/**
 * Read-View einer Umgebung fuer die REST-Schicht (Iteration 25).
 * Keine JPA-Entities ueber die Modulgrenze.
 */
public record EnvironmentView(
        UUID id,
        String key,
        String name,
        EnvironmentStage stage,
        String tenant,
        UUID llmModelProfileId) {}
