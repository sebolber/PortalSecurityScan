package com.ahs.cvm.api.assessment;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Slice-{@code @SpringBootConfiguration} fuer WebMvcTests des
 * Assessments-Controllers. Wird vor {@link com.ahs.cvm.api.TestApiApplication}
 * gefunden, weil sie naeher am Test-Paket liegt. Nur Test-Scope.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.ahs.cvm.api.assessment")
public class AssessmentsTestApi {}
