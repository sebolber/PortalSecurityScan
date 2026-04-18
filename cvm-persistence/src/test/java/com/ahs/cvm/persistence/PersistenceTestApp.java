package com.ahs.cvm.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Test-{@link SpringBootConfiguration}, damit {@code @DataJpaTest}-basierte
 * Slice-Tests in {@code cvm-persistence} eine Bootstrap-Klasse finden.
 *
 * <p>Ohne diese Klasse scheitern Slice-Tests mit „Unable to find a
 * &#64;SpringBootConfiguration", sobald Docker verfuegbar ist und der
 * {@code EnabledIf}-Guard die Tests tatsaechlich aktiviert. Die regulaere
 * {@link PersistenceConfig} ist nur ein {@code &#64;Configuration} ohne
 * Auto-Configuration-Enablement und reicht fuer das JPA-Slicing nicht.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.ahs.cvm.persistence")
public class PersistenceTestApp {}
