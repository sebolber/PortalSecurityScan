package com.ahs.cvm.application.parameter;

import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.persistence.parameter.SystemParameter;
import com.ahs.cvm.persistence.parameter.SystemParameterRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liest effektive Werte aus dem System-Parameter-Store (mit Fallback auf die
 * vom Aufrufer uebergebenen Defaults).
 *
 * <p>Lese-Reihenfolge: DB-Wert des aktuellen Tenants &rarr; uebergebener
 * Fallback. Ohne Tenant-Kontext wird direkt der Fallback zurueckgegeben,
 * damit Hintergrund-Jobs ohne aktiven Tenant nicht fehlschlagen.
 */
@Service
@RequiredArgsConstructor
public class SystemParameterResolver {

    private final SystemParameterRepository parameterRepository;

    @Transactional(readOnly = true)
    public Optional<String> resolve(String paramKey) {
        Optional<UUID> tenantId = TenantContext.current();
        if (tenantId.isEmpty()) {
            return Optional.empty();
        }
        return parameterRepository
                .findByTenantIdAndParamKey(tenantId.get(), paramKey)
                .map(SystemParameter::getValue)
                .filter(v -> v != null && !v.isBlank());
    }

    public String resolveString(String paramKey, String fallback) {
        return resolve(paramKey).orElse(fallback);
    }

    public boolean resolveBoolean(String paramKey, boolean fallback) {
        return resolve(paramKey)
                .map(v -> v.trim().toLowerCase())
                .map(v -> v.equals("true") || v.equals("1") || v.equals("yes"))
                .orElse(fallback);
    }

    public int resolveInt(String paramKey, int fallback) {
        return resolve(paramKey)
                .map(String::trim)
                .map(v -> {
                    try {
                        return Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    public long resolveLong(String paramKey, long fallback) {
        return resolve(paramKey)
                .map(String::trim)
                .map(v -> {
                    try {
                        return Long.parseLong(v);
                    } catch (NumberFormatException e) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    public double resolveDouble(String paramKey, double fallback) {
        return resolve(paramKey)
                .map(String::trim)
                .map(v -> {
                    try {
                        return new BigDecimal(v).doubleValue();
                    } catch (NumberFormatException e) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }
}
