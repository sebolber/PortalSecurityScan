package com.ahs.cvm.application.llmconfig;

import java.util.UUID;

/**
 * Eingabe fuer den {@link LlmConfigurationTester}.
 *
 * <p>Wird ein {@link #id} angegeben, laedt der Service die gespeicherte
 * Konfiguration und ergaenzt fehlende Felder (inkl. entschluesseltes
 * Secret); so muss der Admin sein Secret nicht erneut eintippen, um
 * eine bestehende Konfiguration zu pruefen. Ist {@link #id}
 * {@code null}, wird ausschliesslich mit den uebergebenen Werten
 * getestet.
 */
public record LlmConfigurationTestCommand(
        UUID id,
        String provider,
        String model,
        String baseUrl,
        String apiKey) {}
