package com.ahs.cvm.api.rules;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Slice-{@code @SpringBootConfiguration} fuer {@code @WebMvcTest}s im
 * Rules-Paket. Wird vor {@link com.ahs.cvm.api.TestApiApplication}
 * gefunden, weil sie naeher am Test-Paket liegt. Nur Test-Scope.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.ahs.cvm.api.rules")
public class RulesTestApi {}
