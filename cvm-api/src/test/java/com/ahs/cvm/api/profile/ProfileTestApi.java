package com.ahs.cvm.api.profile;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Slice-{@code @SpringBootConfiguration} fuer {@code @WebMvcTest}-Tests im
 * Profile-Paket. Wird automatisch vor {@link com.ahs.cvm.api.TestApiApplication}
 * gefunden, da sie sich naeher am Test-Paket befindet. Nur Test-Scope.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.ahs.cvm.api.profile")
public class ProfileTestApi {}
