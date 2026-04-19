package com.ahs.cvm.application.environment;

import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.domain.enums.EnvironmentStage;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read- und Anlage-Service fuer die {@code environment}-Tabelle
 * (Iteration 25, CVM-56 + Iteration 28e, CVM-69).
 *
 * <p>Iteration 62B (CVM-62): Tenant-Scope. Lesen liefert nur Umgebungen
 * des aktuellen {@link TenantContext}; Anlage setzt die Mandanten-ID
 * automatisch, `loesche` weist fremde Eintraege ab.
 */
@Service
public class EnvironmentQueryService {

    private final EnvironmentRepository repository;

    public EnvironmentQueryService(EnvironmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<EnvironmentView> listAll() {
        return TenantContext.current()
                .map(tenantId -> repository
                        .findByTenantIdAndDeletedAtIsNullOrderByKeyAsc(tenantId)
                        .stream()
                        .map(EnvironmentQueryService::toView)
                        .toList())
                .orElse(List.of());
    }

    @Transactional
    public void loesche(UUID environmentId) {
        if (environmentId == null) {
            throw new IllegalArgumentException("environmentId darf nicht null sein.");
        }
        Environment umgebung = repository.findById(environmentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Umgebung nicht gefunden: " + environmentId));
        pruefeTenantZugehoerigkeit(umgebung);
        if (umgebung.getDeletedAt() == null) {
            umgebung.setDeletedAt(Instant.now());
            repository.save(umgebung);
        }
    }

    @Transactional
    public EnvironmentView create(CreateEnvironmentCommand command) {
        String key = requireText(command.key(), "key");
        String name = requireText(command.name(), "name");
        EnvironmentStage stage = command.stage();
        if (stage == null) {
            throw new IllegalArgumentException("stage fehlt.");
        }
        UUID tenantId = TenantContext.requireCurrent();
        if (repository.findByTenantIdAndKey(tenantId, key).isPresent()) {
            throw new EnvironmentKeyAlreadyExistsException(key);
        }
        Environment saved = repository.save(
                Environment.builder()
                        .tenantId(tenantId)
                        .key(key)
                        .name(name)
                        .stage(stage)
                        .tenant(trimOrNull(command.tenant()))
                        .build());
        return toView(saved);
    }

    private static String requireText(String value, String feldname) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    feldname + " darf nicht leer sein.");
        }
        return value.trim();
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void pruefeTenantZugehoerigkeit(Environment umgebung) {
        TenantContext.current().ifPresent(tenantId -> {
            if (!tenantId.equals(umgebung.getTenantId())) {
                throw new EntityNotFoundException(
                        "Umgebung nicht gefunden: " + umgebung.getId());
            }
        });
    }

    static EnvironmentView toView(Environment e) {
        return new EnvironmentView(
                e.getId(),
                e.getKey(),
                e.getName(),
                e.getStage(),
                e.getTenant(),
                e.getLlmModelProfileId());
    }

    /** Command fuer die Anlage einer neuen Umgebung. */
    public record CreateEnvironmentCommand(
            String key, String name, EnvironmentStage stage, String tenant) {}

    /** Wird geworfen, wenn ein {@code key} bereits existiert. */
    public static final class EnvironmentKeyAlreadyExistsException
            extends RuntimeException {
        public EnvironmentKeyAlreadyExistsException(String key) {
            super("Umgebung mit key '" + key + "' existiert bereits.");
        }
    }
}
