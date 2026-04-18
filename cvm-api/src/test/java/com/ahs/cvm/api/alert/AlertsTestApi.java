package com.ahs.cvm.api.alert;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/** Slice-Konfig fuer den AlertsController. */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.ahs.cvm.api.alert")
public class AlertsTestApi {}
