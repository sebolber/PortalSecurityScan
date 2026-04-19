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

    /**
     * Iteration 59 (CVM-109): Neuer Mandant. Die Aktivierung im
     * Keycloak-Mapping ist nicht Teil dieses Use-Cases und folgt
     * ueber Realm-Setup. Wirft
     * {@link TenantKeyAlreadyExistsException}, wenn der Key bereits
     * existiert.
     */
    @Transactional
    public TenantView create(String tenantKey, String name, boolean active) {
        if (tenantKey == null || tenantKey.isBlank()) {
            throw new IllegalArgumentException("tenantKey darf nicht leer sein.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name darf nicht leer sein.");
        }
        String key = tenantKey.trim();
        if (tenantRepository.findByTenantKey(key).isPresent()) {
            throw new TenantKeyAlreadyExistsException(key);
        }
        Tenant saved = tenantRepository.save(Tenant.builder()
                .tenantKey(key)
                .name(name.trim())
                .active(active)
                .defaultTenant(false)
                .build());
        return TenantView.from(saved);
    }

    /**
     * Iteration 60 (CVM-110): Active-Flag umschalten. Der Default-
     * Mandant darf nicht deaktiviert werden (sonst fehlt der
     * Fallback fuer JWTs ohne tenant_key).
     */
    @Transactional
    public TenantView setActive(UUID tenantId, boolean active) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId darf nicht null sein.");
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Mandant nicht gefunden: " + tenantId));
        if (!active && tenant.isDefaultTenant()) {
            throw new IllegalStateException(
                    "Default-Mandant kann nicht deaktiviert werden.");
        }
        if (tenant.isActive() != active) {
            tenant.setActive(active);
            tenantRepository.save(tenant);
        }
        return TenantView.from(tenant);
    }

    public static final class TenantKeyAlreadyExistsException extends RuntimeException {
        public TenantKeyAlreadyExistsException(String key) {
            super("Mandant mit key '" + key + "' existiert bereits.");
        }
    }
}
