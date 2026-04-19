package com.ahs.cvm.application.parameter;

import com.ahs.cvm.domain.enums.SystemParameterType;
import com.ahs.cvm.persistence.parameter.SystemParameter;

import java.time.Instant;
import java.util.UUID;

public record SystemParameterView(
        UUID id,
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
        boolean adminOnly,
        Instant createdAt,
        Instant updatedAt,
        String updatedBy,
        boolean restartRequired
) {

    private static final String MASKED = "***";

    public static SystemParameterView from(SystemParameter entity) {
        return from(entity, restartRequiredFromCatalog(entity.getParamKey()));
    }

    public static SystemParameterView from(SystemParameter entity, boolean restartRequired) {
        String displayedValue = entity.isSensitive() && entity.getValue() != null && !entity.getValue().isBlank()
                ? MASKED
                : entity.getValue();
        return new SystemParameterView(
                entity.getId(),
                entity.getParamKey(),
                entity.getLabel(),
                entity.getDescription(),
                entity.getHandbook(),
                entity.getCategory(),
                entity.getSubcategory(),
                entity.getType(),
                displayedValue,
                entity.getDefaultValue(),
                entity.isRequired(),
                entity.getValidationRules(),
                entity.getOptions(),
                entity.getUnit(),
                entity.isSensitive(),
                entity.isHotReload(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.isAdminOnly(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy(),
                restartRequired
        );
    }

    private static boolean restartRequiredFromCatalog(String paramKey) {
        return SystemParameterCatalog.entries().stream()
                .filter(e -> e.paramKey().equals(paramKey))
                .findFirst()
                .map(SystemParameterCatalogEntry::restartRequired)
                .orElse(false);
    }
}
