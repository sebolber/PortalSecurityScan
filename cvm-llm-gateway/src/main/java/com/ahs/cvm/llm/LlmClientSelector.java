package com.ahs.cvm.llm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Waehlt den passenden {@link LlmClient} fuer (Umgebung, Use-Case).
 *
 * <p>Die eigentliche Mapping-Logik wird von einem injizierten
 * {@link EnvironmentModelResolver} geliefert. Ein Test kann diesen
 * Resolver mocken, um z.B. "Umgebung X -&gt; Ollama, sonst Claude" zu
 * pruefen. Die Default-Implementierung sagt immer
 * {@link LlmGatewayConfig#defaultModel()}.
 *
 * <p>Seit Iteration 34c beruecksichtigt der Selector zuerst die
 * aktive {@link TenantLlmSettings} (wenn gesetzt): der Adapter wird
 * anhand des Provider-Schluessels gewaehlt, der EnvironmentModelResolver
 * greift nur noch als Fallback.
 */
@Component
public class LlmClientSelector {

    private static final Logger log = LoggerFactory.getLogger(LlmClientSelector.class);

    private final List<LlmClient> clients;
    private final EnvironmentModelResolver resolver;
    private final LlmGatewayConfig config;
    private final Optional<TenantLlmSettingsProvider> tenantProvider;

    public LlmClientSelector(
            List<LlmClient> clients,
            Optional<EnvironmentModelResolver> resolver,
            LlmGatewayConfig config,
            Optional<TenantLlmSettingsProvider> tenantProvider) {
        this.clients = List.copyOf(clients);
        this.resolver = resolver.orElseGet(() ->
                (env, useCase) -> config.defaultModel());
        this.config = config;
        this.tenantProvider = tenantProvider;
    }

    /**
     * Liefert einen Client. Reihenfolge:
     * <ol>
     *   <li>Aktive Tenant-Konfig (Provider-Match ueber
     *       {@link LlmClient#provider()}).</li>
     *   <li>Fallback: Modell-Match via {@link EnvironmentModelResolver}.</li>
     * </ol>
     *
     * @throws NoLlmClientForModelException wenn nichts passt.
     */
    public LlmClient select(UUID environmentId, String useCase) {
        Optional<TenantLlmSettings> tenant = tenantProvider
                .flatMap(TenantLlmSettingsProvider::resolveCurrent);
        if (tenant.isPresent()) {
            String providerKey = tenant.get().provider();
            Optional<LlmClient> byProvider = clients.stream()
                    .filter(c -> c.supportsProvider(providerKey))
                    .findFirst();
            if (byProvider.isPresent()) {
                return byProvider.get();
            }
            log.warn(
                    "Kein LlmClient fuer Tenant-Provider '{}' registriert. Fallback auf EnvironmentModelResolver.",
                    providerKey);
        }
        String modelId = resolver.resolve(environmentId, useCase);
        return clients.stream()
                .filter(c -> c.modelId().equals(modelId))
                .findFirst()
                .orElseThrow(() -> new NoLlmClientForModelException(modelId));
    }

    /** Pluggable Resolver: (Umgebung, Use-Case) -&gt; Modell-Id. */
    @FunctionalInterface
    public interface EnvironmentModelResolver {
        String resolve(UUID environmentId, String useCase);
    }

    public static class NoLlmClientForModelException extends RuntimeException {
        public NoLlmClientForModelException(String modelId) {
            super("Kein LlmClient fuer Modell registriert: " + modelId);
        }
    }

    /** Einfacher Mapping-Resolver anhand eines statischen Mappings. */
    public static EnvironmentModelResolver staticMapping(
            Function<UUID, String> mappingOrNull, String fallback) {
        return (env, useCase) -> {
            if (env == null) {
                return fallback;
            }
            String m = mappingOrNull.apply(env);
            return m == null ? fallback : m;
        };
    }
}
