package com.ahs.cvm.persistence;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Aktiviert Entity- und Repository-Scan fuer alle Sub-Pakete unterhalb
 * {@code com.ahs.cvm.persistence}. Wird ueber Komponenten-Scan des
 * {@code cvm-app} automatisch aufgegriffen.
 */
@Configuration
@EntityScan(basePackages = "com.ahs.cvm.persistence")
@EnableJpaRepositories(basePackages = "com.ahs.cvm.persistence")
public class PersistenceConfig {}
