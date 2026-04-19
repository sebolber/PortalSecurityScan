package com.ahs.cvm.application.llmconfig;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Provider-Whitelist und Default-Base-URLs nach Spec (Iteration 34,
 * CVM-78).
 *
 * <p>Zentrale Stelle, damit Service und Controller dieselben Werte
 * verwenden und Konzept-Aenderungen (neuer Provider) an einer
 * einzigen Stelle gemacht werden.
 */
public final class ProviderDefaults {

    /** Zulaessige Provider-Schluessel (case-insensitive beim Input). */
    public static final Set<String> PROVIDERS = Set.of(
            "openai", "anthropic", "azure", "ollama", "adesso-ai-hub");

    private static final Map<String, String> DEFAULT_BASE_URLS = Map.of(
            "openai", "https://api.openai.com/v1",
            "anthropic", "https://api.anthropic.com/v1",
            "ollama", "http://localhost:11434/v1",
            "adesso-ai-hub", "https://adesso-ai-hub.3asabc.de/v1");

    private ProviderDefaults() {}

    public static String normalize(String provider) {
        return provider == null
                ? null
                : provider.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isKnown(String provider) {
        return PROVIDERS.contains(normalize(provider));
    }

    /**
     * Liefert die Default-Base-URL fuer einen Provider. Azure hat
     * bewusst keinen Default - der Admin muss eine konkrete URL
     * angeben.
     */
    public static Optional<String> defaultBaseUrl(String provider) {
        return Optional.ofNullable(DEFAULT_BASE_URLS.get(normalize(provider)));
    }

    public static boolean requiresExplicitBaseUrl(String provider) {
        return "azure".equals(normalize(provider));
    }
}
