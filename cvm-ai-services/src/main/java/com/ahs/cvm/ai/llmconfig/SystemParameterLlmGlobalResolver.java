package com.ahs.cvm.ai.llmconfig;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import com.ahs.cvm.llm.config.LlmGlobalParameterResolver;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter-Bruecke zwischen dem {@link LlmGlobalParameterResolver}-
 * Port (Iteration 66, CVM-303) und dem im {@code cvm-application}-
 * Modul lebenden {@link SystemParameterResolver}.
 *
 * <p>Liegt bewusst in {@code cvm-ai-services}, weil nur dieses
 * Modul sowohl {@code cvm.application..} als auch
 * {@code cvm.llm..} sehen darf - die LLM-Adapter bleiben frei von
 * Persistenz- und Secret-Cipher-Abhaengigkeiten.
 */
@Component
public class SystemParameterLlmGlobalResolver
        implements LlmGlobalParameterResolver {

    private final SystemParameterResolver resolver;

    public SystemParameterLlmGlobalResolver(SystemParameterResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public Optional<String> resolve(String paramKey) {
        return resolver.resolve(paramKey);
    }
}
