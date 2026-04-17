package com.ahs.cvm.api;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Minimal-{@code @SpringBootConfiguration} fuer {@code @WebMvcTest}-Slices im
 * cvm-api-Modul, damit der Test-Bootstrap nicht auf {@code cvm-app} angewiesen
 * ist. Nur Test-Scope.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.ahs.cvm.api.scan")
public class TestApiApplication {}
