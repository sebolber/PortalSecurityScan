package com.ahs.cvm.llm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Waehlt den passenden {@link LlmClient} fuer (Umgebung, Use-Case).
 *
 * <p>Die eigentliche Mapping-Logik wird von einem injizierten
 * {@link EnvironmentModelResolver} geliefert. Ein Test kann diesen
 * Resolver mocken, um z.B. "Umgebung X -&gt; Ollama, sonst Claude" zu
 * pruefen. Die Default-Implementierung sagt immer
 * {@link LlmGatewayConfig#defaultModel()}.
 */
@Component
public class LlmClientSelector {

    private final List<LlmClient> clients;
    private final EnvironmentModelResolver resolver;
    private final LlmGatewayConfig config;

    public LlmClientSelector(
            List<LlmClient> clients,
            Optional<EnvironmentModelResolver> resolver,
            LlmGatewayConfig config) {
        this.clients = List.copyOf(clients);
        this.resolver = resolver.orElseGet(() ->
                (env, useCase) -> config.defaultModel());
        this.config = config;
    }

    /**
     * Liefert einen Client, der {@code modelId} unterstuetzt.
     * @throws NoLlmClientForModelException wenn nichts passt.
     */
    public LlmClient select(UUID environmentId, String useCase) {
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
