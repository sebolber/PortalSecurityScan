package com.ahs.cvm.application.parameter;

import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.persistence.parameter.SystemParameter;
import com.ahs.cvm.persistence.parameter.SystemParameterAuditLog;
import com.ahs.cvm.persistence.parameter.SystemParameterAuditLogRepository;
import com.ahs.cvm.persistence.parameter.SystemParameterRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemParameterService {

    private final SystemParameterRepository parameterRepository;
    private final SystemParameterAuditLogRepository auditLogRepository;
    private final SystemParameterValidator validator;

    public List<SystemParameterView> list(String category) {
        UUID tenantId = currentTenant();
        List<SystemParameter> entities = (category == null || category.isBlank())
                ? parameterRepository.findByTenantIdOrderByCategoryAscLabelAsc(tenantId)
                : parameterRepository.findByTenantIdAndCategoryOrderByLabelAsc(tenantId, category);
        return entities.stream().map(SystemParameterView::from).toList();
    }

    public SystemParameterView get(UUID id) {
        return SystemParameterView.from(load(id));
    }

    @Transactional
    public SystemParameterView create(SystemParameterCommands.CreateCommand cmd, String actor) {
        UUID tenantId = currentTenant();
        parameterRepository.findByTenantIdAndParamKey(tenantId, cmd.paramKey()).ifPresent(p -> {
            throw new IllegalArgumentException("Parameter mit Schlüssel '" + cmd.paramKey() + "' existiert bereits");
        });

        SystemParameter entity = SystemParameter.builder()
                .paramKey(cmd.paramKey())
                .label(cmd.label())
                .description(cmd.description())
                .handbook(cmd.handbook())
                .category(cmd.category())
                .subcategory(cmd.subcategory())
                .type(cmd.type())
                .value(cmd.value())
                .defaultValue(cmd.defaultValue())
                .required(cmd.required())
                .validationRules(cmd.validationRules())
                .options(cmd.options())
                .unit(cmd.unit())
                .sensitive(cmd.sensitive())
                .hotReload(cmd.hotReload())
                .validFrom(cmd.validFrom())
                .validTo(cmd.validTo())
                .adminOnly(cmd.adminOnly())
                .tenantId(tenantId)
                .updatedBy(actor)
                .build();

        validator.validate(entity, entity.getValue());
        SystemParameter saved = parameterRepository.save(entity);

        if (saved.getValue() != null && !saved.getValue().isBlank()) {
            writeAudit(saved, null, saved.getValue(), actor, "Anlage");
        }
        return SystemParameterView.from(saved);
    }

    @Transactional
    public SystemParameterView update(UUID id, SystemParameterCommands.UpdateCommand cmd, String actor) {
        SystemParameter entity = load(id);
        entity.setLabel(cmd.label());
        entity.setDescription(cmd.description());
        entity.setHandbook(cmd.handbook());
        entity.setCategory(cmd.category());
        entity.setSubcategory(cmd.subcategory());
        entity.setType(cmd.type());
        entity.setDefaultValue(cmd.defaultValue());
        entity.setRequired(cmd.required());
        entity.setValidationRules(cmd.validationRules());
        entity.setOptions(cmd.options());
        entity.setUnit(cmd.unit());
        entity.setSensitive(cmd.sensitive());
        entity.setHotReload(cmd.hotReload());
        entity.setValidFrom(cmd.validFrom());
        entity.setValidTo(cmd.validTo());
        entity.setAdminOnly(cmd.adminOnly());
        entity.setUpdatedBy(actor);
        validator.validate(entity, entity.getValue());
        return SystemParameterView.from(parameterRepository.save(entity));
    }

    @Transactional
    public SystemParameterView changeValue(UUID id, SystemParameterCommands.ChangeValueCommand cmd, String actor) {
        SystemParameter entity = load(id);
        validator.validate(entity, cmd.value());
        String oldValue = entity.getValue();
        entity.setValue(cmd.value());
        entity.setUpdatedBy(actor);
        SystemParameter saved = parameterRepository.save(entity);
        if (!Objects.equals(oldValue, cmd.value())) {
            writeAudit(saved, oldValue, cmd.value(), actor, cmd.reason());
        }
        return SystemParameterView.from(saved);
    }

    @Transactional
    public SystemParameterView reset(UUID id, String actor) {
        SystemParameter entity = load(id);
        String oldValue = entity.getValue();
        entity.setValue(entity.getDefaultValue());
        entity.setUpdatedBy(actor);
        SystemParameter saved = parameterRepository.save(entity);
        if (!Objects.equals(oldValue, entity.getDefaultValue())) {
            writeAudit(saved, oldValue, entity.getDefaultValue(), actor, "Reset auf Standardwert");
        }
        return SystemParameterView.from(saved);
    }

    @Transactional
    public void delete(UUID id, String actor) {
        SystemParameter entity = load(id);
        writeAudit(entity, entity.getValue(), null, actor, "Löschung");
        parameterRepository.delete(entity);
    }

    public List<SystemParameterAuditLogView> auditLog(UUID parameterId) {
        UUID tenantId = currentTenant();
        List<SystemParameterAuditLog> entries = parameterId != null
                ? auditLogRepository.findByParameterIdOrderByChangedAtDesc(parameterId)
                : auditLogRepository.findByTenantIdOrderByChangedAtDesc(tenantId);
        return entries.stream()
                .map(e -> SystemParameterAuditLogView.from(e, isSensitive(e.getParameterId())))
                .toList();
    }

    private boolean isSensitive(UUID parameterId) {
        return parameterRepository.findById(parameterId).map(SystemParameter::isSensitive).orElse(false);
    }

    private SystemParameter load(UUID id) {
        UUID tenantId = currentTenant();
        SystemParameter entity = parameterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Parameter nicht gefunden: " + id));
        if (!Objects.equals(entity.getTenantId(), tenantId)) {
            throw new EntityNotFoundException("Parameter nicht gefunden: " + id);
        }
        return entity;
    }

    private void writeAudit(SystemParameter parameter, String oldValue, String newValue, String actor, String reason) {
        boolean sensitive = parameter.isSensitive();
        SystemParameterAuditLog entry = SystemParameterAuditLog.builder()
                .parameterId(parameter.getId())
                .paramKey(parameter.getParamKey())
                .oldValue(sensitive && oldValue != null && !oldValue.isBlank() ? "***" : oldValue)
                .newValue(sensitive && newValue != null && !newValue.isBlank() ? "***" : newValue)
                .changedBy(actor)
                .changedAt(Instant.now())
                .reason(reason)
                .tenantId(parameter.getTenantId())
                .build();
        auditLogRepository.save(entry);
    }

    private UUID currentTenant() {
        return TenantContext.requireCurrent();
    }
}
