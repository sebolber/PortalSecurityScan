package com.ahs.cvm.application.alert;

import com.ahs.cvm.domain.enums.AlertSeverity;
import com.ahs.cvm.domain.enums.AlertTriggerArt;
import com.ahs.cvm.persistence.alert.AlertRule;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-Projektion der {@link AlertRule}. Die API-Schicht arbeitet
 * gegen diesen Record (kein Persistenz-Durchgriff).
 */
public record AlertRuleView(
        UUID id,
        String name,
        String description,
        AlertTriggerArt triggerArt,
        AlertSeverity severity,
        Integer cooldownMinutes,
        String subjectPrefix,
        String templateName,
        List<String> recipients,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    public static AlertRuleView from(AlertRule rule) {
        return new AlertRuleView(
                rule.getId(),
                rule.getName(),
                rule.getDescription(),
                rule.getTriggerArt(),
                rule.getSeverity(),
                rule.getCooldownMinutes(),
                rule.getSubjectPrefix(),
                rule.getTemplateName(),
                rule.getRecipients() == null ? List.of() : List.copyOf(rule.getRecipients()),
                Boolean.TRUE.equals(rule.getEnabled()),
                rule.getCreatedAt(),
                rule.getUpdatedAt());
    }
}
