package com.ahs.cvm.application.parameter;

import com.ahs.cvm.persistence.parameter.SystemParameterAuditLog;

import java.time.Instant;
import java.util.UUID;

public record SystemParameterAuditLogView(
        UUID id,
        UUID parameterId,
        String paramKey,
        String oldValue,
        String newValue,
        String changedBy,
        Instant changedAt,
        String reason
) {

    public static SystemParameterAuditLogView from(SystemParameterAuditLog entry, boolean sensitive) {
        String oldVal = sensitive && entry.getOldValue() != null && !entry.getOldValue().isBlank() ? "***" : entry.getOldValue();
        String newVal = sensitive && entry.getNewValue() != null && !entry.getNewValue().isBlank() ? "***" : entry.getNewValue();
        return new SystemParameterAuditLogView(
                entry.getId(),
                entry.getParameterId(),
                entry.getParamKey(),
                oldVal,
                newVal,
                entry.getChangedBy(),
                entry.getChangedAt(),
                entry.getReason()
        );
    }
}
