package com.ahs.cvm.application.environment;

import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentDeployment;
import com.ahs.cvm.persistence.environment.EnvironmentDeploymentRepository;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verwaltung der Umgebungen und ihrer aktiven Deployments (Produkt-Version
 * pro Umgebung). Skelett; fachliche Logik (Retirement-Autoreplace) folgt
 * in Iteration 02.
 */
@Service
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final EnvironmentDeploymentRepository deploymentRepository;

    public EnvironmentService(
            EnvironmentRepository environmentRepository,
            EnvironmentDeploymentRepository deploymentRepository) {
        this.environmentRepository = environmentRepository;
        this.deploymentRepository = deploymentRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Environment> findeUmgebungUeberKey(String key) {
        return environmentRepository.findByKey(key);
    }

    @Transactional(readOnly = true)
    public List<EnvironmentDeployment> deploymentsFuerUmgebung(UUID umgebungId) {
        return deploymentRepository.findByEnvironmentId(umgebungId);
    }

    @Transactional
    public Environment speichereUmgebung(Environment umgebung) {
        return environmentRepository.save(umgebung);
    }

    @Transactional
    public EnvironmentDeployment speichereDeployment(EnvironmentDeployment deployment) {
        return deploymentRepository.save(deployment);
    }
}
