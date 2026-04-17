package com.ahs.cvm.api.finding;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Slice-{@code @SpringBootConfiguration} fuer WebMvcTests des
 * Findings-Controllers. Nur Test-Scope.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.ahs.cvm.api.finding")
public class FindingsTestApi {}
