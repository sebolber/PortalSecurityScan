package com.ahs.cvm.application.parameter;

import com.ahs.cvm.domain.enums.SystemParameterType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Statischer Katalog aller vom System-Parameter-Store verwalteten Schluessel.
 *
 * <p>Iteration 41 (Block A.1): AI_LLM-Fallbacks, AI_REACHABILITY, RAG,
 * ANOMALY, COPILOT. Weitere Kategorien folgen in Iteration 42.
 *
 * <p>Bewusst nicht aufgenommen: Spring- und Infrastruktur-Schluessel
 * (`spring.datasource.*`, `spring.jpa.*`, `server.port`,
 * `management.endpoints.*`, `logging.level.*`, `cvm.llm.pricing.*`,
 * `cvm.enrichment.osv.base-url`, `cvm.feed.*.base-url`) sowie Entity-
 * artige Konfigurationen (`LlmConfiguration`, `ModelProfile`, `Rule`,
 * `ContextProfile`, `BrandingConfig`, `Tenant`, `Waiver`, `AlertRule`,
 * `Product`, `ProductVersion`, `Environment`). Begruendung siehe
 * {@code docs/20260419/offene-punkte.md}.
 */
public final class SystemParameterCatalog {

    public static final String CATEGORY_AI_LLM = "AI_LLM";
    public static final String CATEGORY_AI_REACHABILITY = "AI_REACHABILITY";
    public static final String CATEGORY_RAG = "RAG";
    public static final String CATEGORY_ANOMALY = "ANOMALY";
    public static final String CATEGORY_COPILOT = "COPILOT";

    private static final List<SystemParameterCatalogEntry> ENTRIES = buildEntries();

    private SystemParameterCatalog() {}

    public static List<SystemParameterCatalogEntry> entries() {
        return ENTRIES;
    }

    private static List<SystemParameterCatalogEntry> buildEntries() {
        List<SystemParameterCatalogEntry> list = new ArrayList<>();
        addAiLlmFallbacks(list);
        addAiReachability(list);
        addRag(list);
        addAnomaly(list);
        addCopilot(list);
        ensureUnique(list);
        return Collections.unmodifiableList(list);
    }

    private static void addAiLlmFallbacks(List<SystemParameterCatalogEntry> list) {
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.enabled",
                "LLM-Gateway aktiv",
                "Globaler Feature-Flag. Wenn false, werden alle LLM-Adapter geblockt.",
                "Default false. In Produktion nur aktivieren, wenn Modell-Profil, Audit und Rate-Limit bestaetigt sind.",
                CATEGORY_AI_LLM, "global",
                SystemParameterType.BOOLEAN, "false", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.injection.mode",
                "Injection-Modus",
                "Verhalten des InjectionDetector: warnen (warn) oder blockieren (block).",
                "warn = Vorschlag traegt Flag injectionRisk; block = Vorschlag wird verworfen.",
                CATEGORY_AI_LLM, "global",
                SystemParameterType.SELECT, "warn", true, null, "warn,block", null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.default-model",
                "Fallback-Modell",
                "Modell-Bezeichner, wenn fuer einen Mandanten keine LlmConfiguration aktiv ist.",
                "Wirkt nur, wenn LlmConfigurationService keinen aktiven Eintrag findet.",
                CATEGORY_AI_LLM, "global",
                SystemParameterType.STRING, "claude-sonnet-4-6", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.rate-limit.global-per-minute",
                "Globales Rate-Limit (pro Minute)",
                "Maximale Anzahl LLM-Calls systemweit pro Minute (Bucket4j).",
                null,
                CATEGORY_AI_LLM, "rate-limit",
                SystemParameterType.INTEGER, "120", true, null, null, "calls/min", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.rate-limit.tenant-per-minute",
                "Mandanten-Rate-Limit (pro Minute)",
                "Maximale Anzahl LLM-Calls pro Mandant und Minute.",
                null,
                CATEGORY_AI_LLM, "rate-limit",
                SystemParameterType.INTEGER, "30", true, null, null, "calls/min", false, true, true));

        // Claude-Fallback-Adapter (Konfig ohne api-key; Secret folgt Iteration 45)
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.claude.version",
                "Claude API-Version",
                "Wert fuer den Header anthropic-version im Claude-Adapter.",
                null,
                CATEGORY_AI_LLM, "claude",
                SystemParameterType.STRING, "2023-06-01", true, null, null, null, false, false, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.claude.timeout-seconds",
                "Claude HTTP-Timeout",
                "Maximale Wartezeit fuer einen einzelnen Claude-API-Call.",
                null,
                CATEGORY_AI_LLM, "claude",
                SystemParameterType.INTEGER, "30", true, null, null, "Sekunden", false, false, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.claude.model",
                "Claude-Fallback-Modell",
                "Modell-Bezeichner fuer den Claude-Adapter, wenn kein Profil existiert.",
                null,
                CATEGORY_AI_LLM, "claude",
                SystemParameterType.STRING, "claude-sonnet-4-6", true, null, null, null, false, false, true));

        // Ollama-Adapter
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.ollama.base-url",
                "Ollama-Basis-URL",
                "HTTP-Basis-URL des Ollama-Servers (on-prem).",
                null,
                CATEGORY_AI_LLM, "ollama",
                SystemParameterType.URL, "http://ollama:11434", true, null, null, null, false, false, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.ollama.model",
                "Ollama-Fallback-Modell",
                "Modell-Bezeichner fuer den Ollama-Adapter, wenn kein Profil existiert.",
                null,
                CATEGORY_AI_LLM, "ollama",
                SystemParameterType.STRING, "llama3.1:8b-instruct", true, null, null, null, false, false, true));

        // OpenAI-kompatibler Adapter (nur Default-Modell, kein Key)
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.openai.default-model",
                "OpenAI-Fallback-Modell",
                "Modell-Bezeichner fuer den OpenAI-kompatiblen Adapter.",
                null,
                CATEGORY_AI_LLM, "openai",
                SystemParameterType.STRING, "gpt-4o-mini", true, null, null, null, false, false, true));

        // Embedding-Adapter
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.embedding.ollama.base-url",
                "Embedding-Ollama-Basis-URL",
                "HTTP-Basis-URL des Ollama-Servers fuer Embeddings (RAG).",
                null,
                CATEGORY_AI_LLM, "embedding",
                SystemParameterType.URL, "http://ollama:11434", true, null, null, null, false, false, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.embedding.ollama.model",
                "Embedding-Modell",
                "Modell-Bezeichner fuer Embeddings (RAG).",
                null,
                CATEGORY_AI_LLM, "embedding",
                SystemParameterType.STRING, "nomic-embed-text", true, null, null, null, false, false, true));
    }

    private static void addAiReachability(List<SystemParameterCatalogEntry> list) {
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.reachability.enabled",
                "Reachability-Agent aktiv",
                "Feature-Flag fuer den Reachability-Agent (Iteration 15).",
                "Wenn false, lehnt der Agent Anfragen ab; Symbol-Vorschlag bleibt verfuegbar.",
                CATEGORY_AI_REACHABILITY, "agent",
                SystemParameterType.BOOLEAN, "false", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.reachability.timeout-seconds",
                "Reachability-Timeout",
                "Maximale Laufzeit eines Reachability-Subprozesses.",
                null,
                CATEGORY_AI_REACHABILITY, "agent",
                SystemParameterType.INTEGER, "300", true, null, null, "Sekunden", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.reachability.binary",
                "Reachability-Binary",
                "Pfad/Name des ausfuehrbaren Reachability-Agenten.",
                null,
                CATEGORY_AI_REACHABILITY, "agent",
                SystemParameterType.STRING, "claude", true, null, null, null, false, false, true));
    }

    private static void addRag(List<SystemParameterCatalogEntry> list) {
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.auto-assessment.enabled",
                "Auto-Bewertung aktiv",
                "Feature-Flag fuer die RAG-gestuetzte KI-Vorbewertung (Iteration 13).",
                "Bei false werden keine AI-Vorschlaege zum Listing erstellt; manuelle Bewertung bleibt moeglich.",
                CATEGORY_RAG, "auto-assessment",
                SystemParameterType.BOOLEAN, "false", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.auto-assessment.top-k",
                "RAG Top-K",
                "Anzahl der naechsten Nachbarn, die der Retriever pro Query zurueckliefert.",
                null,
                CATEGORY_RAG, "auto-assessment",
                SystemParameterType.INTEGER, "5", true, null, null, "Treffer", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.auto-assessment.min-rag-score",
                "RAG Minimal-Score",
                "Untergrenze fuer die Aehnlichkeit; Treffer darunter werden verworfen.",
                null,
                CATEGORY_RAG, "auto-assessment",
                SystemParameterType.DECIMAL, "0.6", true, null, null, null, false, true, true));
    }

    private static void addAnomaly(List<SystemParameterCatalogEntry> list) {
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.anomaly.enabled",
                "Anomalie-Check aktiv",
                "Feature-Flag fuer den Anomalie-Check (Iteration 18).",
                null,
                CATEGORY_ANOMALY, "check",
                SystemParameterType.BOOLEAN, "false", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.anomaly.kev-epss-threshold",
                "KEV/EPSS-Schwelle",
                "EPSS-Schwelle fuer das KEV/NOT_APPLICABLE-Muster.",
                null,
                CATEGORY_ANOMALY, "check",
                SystemParameterType.DECIMAL, "0.7", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.anomaly.many-accept-risk-threshold",
                "Many-Accept-Risk-Schwelle",
                "Waiver-Haeufung pro Bewerter in 24 h, ab der Alarm ausgeloest wird.",
                null,
                CATEGORY_ANOMALY, "check",
                SystemParameterType.INTEGER, "5", true, null, null, "Waiver/24h", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.anomaly.similar-rejection-threshold",
                "Similar-Rejection-Schwelle",
                "String-Aehnlichkeit zur abgelehnten Begruendung (0 bis 1).",
                null,
                CATEGORY_ANOMALY, "check",
                SystemParameterType.DECIMAL, "0.9", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.anomaly.use-llm-second-stage",
                "LLM-Zweitpruefung",
                "Zweite Stufe via LLM-Call (kostet Tokens).",
                null,
                CATEGORY_ANOMALY, "check",
                SystemParameterType.BOOLEAN, "false", true, null, null, null, false, true, true));
    }

    private static void addCopilot(List<SystemParameterCatalogEntry> list) {
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.copilot.enabled",
                "Copilot aktiv",
                "Feature-Flag fuer den Bewerter-Copilot (Iteration 14).",
                "Bei false ist der Copilot-Button im Queue-Detail ausgeblendet.",
                CATEGORY_COPILOT, "chat",
                SystemParameterType.BOOLEAN, "false", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.copilot.model-category",
                "Copilot-Modell-Kategorie",
                "LLM-Kategorie, die der Copilot verwendet (z.B. standard, reasoning).",
                null,
                CATEGORY_COPILOT, "chat",
                SystemParameterType.STRING, "standard", true, null, null, null, false, true, true));
    }

    private static void ensureUnique(List<SystemParameterCatalogEntry> list) {
        Set<String> seen = new HashSet<>();
        for (SystemParameterCatalogEntry e : list) {
            if (!seen.add(e.paramKey())) {
                throw new IllegalStateException("Dublette im Parameter-Katalog: " + e.paramKey());
            }
        }
    }
}
