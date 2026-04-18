package com.ahs.cvm.api.report;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/** Slice-Konfig fuer den ReportsController. */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.ahs.cvm.api.report")
public class ReportsTestApi {}
