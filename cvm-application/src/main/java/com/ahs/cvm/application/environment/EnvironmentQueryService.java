package com.ahs.cvm.application.environment;

import com.ahs.cvm.domain.enums.EnvironmentStage;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read- und Anlage-Service fuer die {@code environment}-Tabelle
 * (Iteration 25, CVM-56 + Iteration 28e, CVM-69). Deckt die Read-
 * und Create-Use-Cases der Einstellungen- und Profile-UI ab, ohne
 * dass {@code cvm-api} direkt auf die Persistence-Entity zugreift.
 */
@Service
public class EnvironmentQueryService {

    private final EnvironmentRepository repository;

    public EnvironmentQueryService(EnvironmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<EnvironmentView> listAll() {
        return repository.findAll().stream()
                .sorted((a, b) -> a.getKey().compareToIgnoreCase(b.getKey()))
                .map(EnvironmentQueryService::toView)
                .toList();
    }

    /**
     * Legt eine neue Umgebung an. {@code key} muss eindeutig sein
     * (DB-Constraint). Wirft {@link EnvironmentKeyAlreadyExistsException},
     * wenn ein Eintrag mit dem Key bereits existiert.
     */
    @Transactional
    public EnvironmentView create(CreateEnvironmentCommand command) {
        String key = requireText(command.key(), "key");
        String name = requireText(command.name(), "name");
        EnvironmentStage stage = command.stage();
        if (stage == null) {
            throw new IllegalArgumentException("stage fehlt.");
        }
        if (repository.findByKey(key).isPresent()) {
            throw new EnvironmentKeyAlreadyExistsException(key);
        }
        Environment saved = repository.save(
                Environment.builder()
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
