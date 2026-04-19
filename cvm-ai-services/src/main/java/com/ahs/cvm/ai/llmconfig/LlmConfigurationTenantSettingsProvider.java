package com.ahs.cvm.ai.llmconfig;

import com.ahs.cvm.application.llmconfig.LlmConfigurationService;
import com.ahs.cvm.application.llmconfig.LlmConfigurationView;
import com.ahs.cvm.llm.TenantLlmSettings;
import com.ahs.cvm.llm.TenantLlmSettingsProvider;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Bruecke zwischen {@link LlmConfigurationService} (CRUD + Secret-
 * Decrypt) und dem {@link TenantLlmSettingsProvider}-Vertrag des
 * {@code cvm-llm-gateway}-Moduls (Iteration 34c, CVM-78).
 *
 * <p>Liegt bewusst im {@code cvm-ai-services}-Modul, weil nur dieses
 * sowohl {@code cvm-application} als auch {@code cvm-llm-gateway}
 * sehen darf. Das LLM-Gateway bleibt so weiterhin frei von
 * Persistenz-Abhaengigkeiten.
 */
@Component
public class LlmConfigurationTenantSettingsProvider
        implements TenantLlmSettingsProvider {

    private static final Logger log = LoggerFactory.getLogger(
            LlmConfigurationTenantSettingsProvider.class);

    private final LlmConfigurationService service;

    public LlmConfigurationTenantSettingsProvider(
            LlmConfigurationService service) {
        this.service = service;
    }

    @Override
    public Optional<TenantLlmSettings> resolveCurrent() {
        try {
            Optional<LlmConfigurationView> active = service.activeForCurrentTenant();
            if (active.isEmpty()) {
                return Optional.empty();
            }
            LlmConfigurationView cfg = active.get();
            String apiKey = cfg.secretSet()
                    ? service.resolveSecret(cfg.id()).orElse(null)
                    : null;
            return Optional.of(new TenantLlmSettings(
                    cfg.provider(),
                    cfg.model(),
                    cfg.baseUrl(),
                    apiKey));
        } catch (RuntimeException ex) {
            // Niemals einen Call scheitern lassen, nur weil die
            // Tenant-Konfig nicht lesbar ist. Default-Spring-Profil-
            // Pfad bleibt aktiv.
            log.warn("TenantLlmSettings konnten nicht aufgeloest werden: {}",
                    ex.getMessage());
            return Optional.empty();
        }
    }
}
