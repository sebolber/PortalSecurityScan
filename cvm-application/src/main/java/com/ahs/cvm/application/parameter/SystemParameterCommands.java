package com.ahs.cvm.application.parameter;

import com.ahs.cvm.domain.enums.SystemParameterType;

import java.time.Instant;

public final class SystemParameterCommands {

    private SystemParameterCommands() {
    }

    public record CreateCommand(
            String paramKey,
            String label,
            String description,
            String handbook,
            String category,
            String subcategory,
            SystemParameterType type,
            String value,
            String defaultValue,
            boolean required,
            String validationRules,
            String options,
            String unit,
            boolean sensitive,
            boolean hotReload,
            Instant validFrom,
            Instant validTo,
            boolean adminOnly
    ) {
    }

    public record UpdateCommand(
            String label,
            String description,
            String handbook,
            String category,
            String subcategory,
            SystemParameterType type,
            String defaultValue,
            boolean required,
            String validationRules,
            String options,
            String unit,
            boolean sensitive,
            boolean hotReload,
            Instant validFrom,
            Instant validTo,
            boolean adminOnly
    ) {
    }

    public record ChangeValueCommand(String value, String reason) {
    }
}
