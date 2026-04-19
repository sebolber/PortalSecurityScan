package com.ahs.cvm.api.parameter;

import com.ahs.cvm.application.parameter.SystemParameterCommands;
import com.ahs.cvm.domain.enums.SystemParameterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class SystemParameterRequests {

    private SystemParameterRequests() {
    }

    public record CreateRequest(
            @NotBlank String paramKey,
            @NotBlank String label,
            String description,
            String handbook,
            @NotBlank String category,
            String subcategory,
            @NotNull SystemParameterType type,
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
        public SystemParameterCommands.CreateCommand toCommand() {
            return new SystemParameterCommands.CreateCommand(
                    paramKey, label, description, handbook, category, subcategory, type,
                    value, defaultValue, required, validationRules, options, unit,
                    sensitive, hotReload, validFrom, validTo, adminOnly
            );
        }
    }

    public record UpdateRequest(
            @NotBlank String label,
            String description,
            String handbook,
            @NotBlank String category,
            String subcategory,
            @NotNull SystemParameterType type,
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
        public SystemParameterCommands.UpdateCommand toCommand() {
            return new SystemParameterCommands.UpdateCommand(
                    label, description, handbook, category, subcategory, type,
                    defaultValue, required, validationRules, options, unit,
                    sensitive, hotReload, validFrom, validTo, adminOnly
            );
        }
    }

    public record ChangeValueRequest(String value, String reason) {
        public SystemParameterCommands.ChangeValueCommand toCommand() {
            return new SystemParameterCommands.ChangeValueCommand(value, reason);
        }
    }
}
