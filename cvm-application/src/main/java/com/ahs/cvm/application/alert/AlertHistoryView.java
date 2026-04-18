package com.ahs.cvm.application.alert;

import com.ahs.cvm.persistence.alert.AlertDispatch;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Lese-Projektion eines Mail-Versands fuer die Alert-Historie im UI
 * (Iteration 27c, CVM-63).
 */
public record AlertHistoryView(
        UUID id,
        UUID ruleId,
        String triggerKey,
        Instant dispatchedAt,
        List<String> recipients,
        String subject,
        String bodyExcerpt,
        boolean dryRun,
        String error) {

    public static AlertHistoryView from(AlertDispatch entity) {
        return new AlertHistoryView(
                entity.getId(),
                entity.getRuleId(),
                entity.getTriggerKey(),
                entity.getDispatchedAt(),
                entity.getRecipients(),
                entity.getSubject(),
                entity.getBodyExcerpt(),
                Boolean.TRUE.equals(entity.getDryRun()),
                entity.getError());
    }
}
