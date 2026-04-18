package com.ahs.cvm.application.environment;

import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-Service fuer die {@code environment}-Tabelle (Iteration 25,
 * CVM-56).  Deckt die Read-Use-Cases der Einstellungen- und
 * Profile-UI ab, ohne dass {@code cvm-api} direkt auf die
 * Persistence-Entity zugreift.
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

    static EnvironmentView toView(Environment e) {
        return new EnvironmentView(
                e.getId(),
                e.getKey(),
                e.getName(),
                e.getStage(),
                e.getTenant(),
                e.getLlmModelProfileId());
    }
}
