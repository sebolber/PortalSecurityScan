package com.ahs.cvm.llm;

import java.util.Optional;

/**
 * Liefert die aktuell aktive {@link TenantLlmSettings} fuer den
 * Mandanten, in dessen Kontext der Call laeuft (Iteration 34c,
 * CVM-78).
 *
 * <p>Die konkrete Implementierung bruecke-t auf
 * {@code LlmConfigurationService} aus dem Application-Modul und
 * haelt damit die Modulgrenze {@code llm-gateway -> domain only}
 * ein.
 */
@FunctionalInterface
public interface TenantLlmSettingsProvider {

    /**
     * Liefert die Settings fuer den aktuellen Mandanten - oder
     * {@link Optional#empty()}, wenn keine aktive Konfiguration
     * gesetzt ist. In diesem Fall bleibt das bisherige Spring-Profil-
     * Verhalten aktiv.
     */
    Optional<TenantLlmSettings> resolveCurrent();
}
