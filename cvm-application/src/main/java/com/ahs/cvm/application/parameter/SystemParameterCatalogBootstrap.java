package com.ahs.cvm.application.parameter;

import com.ahs.cvm.persistence.parameter.SystemParameter;
import com.ahs.cvm.persistence.parameter.SystemParameterRepository;
import com.ahs.cvm.persistence.tenant.Tenant;
import com.ahs.cvm.persistence.tenant.TenantRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sorgt dafuer, dass fuer jeden aktiven Mandanten die im
 * {@link SystemParameterCatalog} deklarierten Eintraege angelegt sind.
 *
 * <p>Bestehende Werte werden nicht veraendert. Fehlende Eintraege werden mit
 * dem {@code defaultValue} als Startwert angelegt. Der Bootstrap laeuft nach
 * {@link ApplicationReadyEvent} und ist idempotent.
 */
@Component
@RequiredArgsConstructor
public class SystemParameterCatalogBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(SystemParameterCatalogBootstrap.class);
    private static final String SYSTEM_ACTOR = "system-bootstrap";

    private final SystemParameterRepository parameterRepository;
    private final TenantRepository tenantRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        seedAllTenants();
    }

    @Transactional
    public int seedAllTenants() {
        List<Tenant> tenants = tenantRepository.findAll().stream()
                .filter(Tenant::isActive)
                .toList();
        if (tenants.isEmpty()) {
            LOG.info("System-Parameter-Katalog: kein aktiver Mandant, nichts zu seeden.");
            return 0;
        }
        int angelegt = 0;
        for (Tenant tenant : tenants) {
            angelegt += seedTenant(tenant.getId());
        }
        LOG.info("System-Parameter-Katalog: {} fehlende Eintraege fuer {} Mandanten angelegt.",
                angelegt, tenants.size());
        return angelegt;
    }

    @Transactional
    public int seedTenant(UUID tenantId) {
        Set<String> vorhanden = parameterRepository
                .findByTenantIdOrderByCategoryAscLabelAsc(tenantId).stream()
                .map(SystemParameter::getParamKey)
                .collect(Collectors.toSet());
        int angelegt = 0;
        for (SystemParameterCatalogEntry entry : SystemParameterCatalog.entries()) {
            if (vorhanden.contains(entry.paramKey())) {
                continue;
            }
            SystemParameter entity = SystemParameter.builder()
                    .tenantId(tenantId)
                    .paramKey(entry.paramKey())
                    .label(entry.label())
                    .description(entry.description())
                    .handbook(entry.handbook())
                    .category(entry.category())
                    .subcategory(entry.subcategory())
                    .type(entry.type())
                    .value(entry.defaultValue())
                    .defaultValue(entry.defaultValue())
                    .required(entry.required())
                    .validationRules(entry.validationRules())
                    .options(entry.options())
                    .unit(entry.unit())
                    .sensitive(entry.sensitive())
                    .hotReload(entry.hotReload())
                    .adminOnly(entry.adminOnly())
                    .updatedBy(SYSTEM_ACTOR)
                    .build();
            parameterRepository.save(entity);
            angelegt++;
        }
        return angelegt;
    }
}
