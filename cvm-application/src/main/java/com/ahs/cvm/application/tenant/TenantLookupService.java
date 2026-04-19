package com.ahs.cvm.application.tenant;

import com.ahs.cvm.persistence.tenant.Tenant;
import com.ahs.cvm.persistence.tenant.TenantRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kapselt den Lese-Zugriff auf die {@code tenant}-Tabelle fuer
 * Module, die selbst nicht auf {@code cvm-persistence} zugreifen
 * duerfen (insbesondere {@code cvm-api}).
 *
 * <p>Eingefuehrt in Iteration 22 zusammen mit dem
 * {@code TenantContextFilter}, der den aktuellen Mandanten aus
 * dem JWT ableitet.
 */
@Service
public class TenantLookupService {

    private final TenantRepository tenantRepository;

    public TenantLookupService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findIdByKey(String tenantKey) {
        if (tenantKey == null || tenantKey.isBlank()) {
            return Optional.empty();
        }
        return tenantRepository.findByTenantKey(tenantKey.trim())
                .filter(Tenant::isActive)
                .map(Tenant::getId);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findDefaultTenantId() {
        return tenantRepository.findFirstByDefaultTenantTrue()
                .filter(Tenant::isActive)
                .map(Tenant::getId);
    }

    /**
     * Iteration 56 (CVM-106): Lese-Liste aller Mandanten fuer die
     * Admin-UI. Sortierung: Default-Tenant zuerst, dann alphabetisch
     * nach Key.
     */
    @Transactional(readOnly = true)
    public List<TenantView> listAll() {
        return tenantRepository.findAll().stream()
                .sorted(Comparator
                        .comparing(Tenant::isDefaultTenant).reversed()
                        .thenComparing(Tenant::getTenantKey,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(TenantView::from)
                .toList();
    }
}
