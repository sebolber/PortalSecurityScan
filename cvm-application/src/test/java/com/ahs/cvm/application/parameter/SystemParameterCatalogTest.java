package com.ahs.cvm.application.parameter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.domain.enums.SystemParameterType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemParameterCatalogTest {

    @Test
    @DisplayName("Katalog enthaelt die erwarteten AI_LLM-, AI_REACHABILITY-, RAG-, ANOMALY- und COPILOT-Schluessel")
    void katalog_enthaelt_alle_block_a1_schluessel() {
        Set<String> keys = paramKeys();
        assertThat(keys).contains(
                // AI_LLM Fallbacks
                "cvm.llm.enabled",
                "cvm.llm.injection.mode",
                "cvm.llm.default-model",
                "cvm.llm.rate-limit.global-per-minute",
                "cvm.llm.rate-limit.tenant-per-minute",
                "cvm.llm.claude.version",
                "cvm.llm.claude.timeout-seconds",
                "cvm.llm.claude.model",
                "cvm.llm.ollama.base-url",
                "cvm.llm.ollama.model",
                "cvm.llm.openai.default-model",
                "cvm.llm.embedding.ollama.base-url",
                "cvm.llm.embedding.ollama.model",
                // AI_REACHABILITY
                "cvm.ai.reachability.enabled",
                "cvm.ai.reachability.timeout-seconds",
                "cvm.ai.reachability.binary",
                // RAG (Auto-Assessment)
                "cvm.ai.auto-assessment.enabled",
                "cvm.ai.auto-assessment.top-k",
                "cvm.ai.auto-assessment.min-rag-score",
                // ANOMALY
                "cvm.ai.anomaly.enabled",
                "cvm.ai.anomaly.kev-epss-threshold",
                "cvm.ai.anomaly.many-accept-risk-threshold",
                "cvm.ai.anomaly.similar-rejection-threshold",
                "cvm.ai.anomaly.use-llm-second-stage",
                // COPILOT
                "cvm.ai.copilot.enabled",
                "cvm.ai.copilot.model-category");
    }

    @Test
    @DisplayName("Katalog enthaelt keine Dubletten")
    void katalog_hat_keine_dubletten() {
        List<SystemParameterCatalogEntry> entries = SystemParameterCatalog.entries();
        assertThat(entries).extracting(SystemParameterCatalogEntry::paramKey)
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Keine Secrets in Iteration 41 (sensitive=false fuer alle Block-A1-Keys)")
    void keine_secrets_in_iteration_41() {
        assertThat(SystemParameterCatalog.entries())
                .allMatch(e -> !e.sensitive(),
                        "Iteration 41 darf keine sensiblen Parameter anlegen; Secrets folgen in Iteration 45");
    }

    @Test
    @DisplayName("Default-Werte stimmen mit den @Value-Fallbacks aus LlmGatewayConfig, ReachabilityConfig, AnomalyConfig und AutoAssessmentConfig ueberein")
    void defaults_spiegeln_bestehende_value_fallbacks() {
        assertThat(findEntry("cvm.llm.enabled").defaultValue()).isEqualTo("false");
        assertThat(findEntry("cvm.llm.injection.mode").defaultValue()).isEqualTo("warn");
        assertThat(findEntry("cvm.llm.default-model").defaultValue()).isEqualTo("claude-sonnet-4-6");
        assertThat(findEntry("cvm.llm.rate-limit.global-per-minute").defaultValue()).isEqualTo("120");
        assertThat(findEntry("cvm.llm.rate-limit.tenant-per-minute").defaultValue()).isEqualTo("30");
        assertThat(findEntry("cvm.llm.claude.version").defaultValue()).isEqualTo("2023-06-01");
        assertThat(findEntry("cvm.llm.claude.timeout-seconds").defaultValue()).isEqualTo("30");
        assertThat(findEntry("cvm.llm.claude.model").defaultValue()).isEqualTo("claude-sonnet-4-6");
        assertThat(findEntry("cvm.llm.ollama.base-url").defaultValue()).isEqualTo("http://ollama:11434");
        assertThat(findEntry("cvm.llm.ollama.model").defaultValue()).isEqualTo("llama3.1:8b-instruct");
        assertThat(findEntry("cvm.llm.openai.default-model").defaultValue()).isEqualTo("gpt-4o-mini");
        assertThat(findEntry("cvm.llm.embedding.ollama.base-url").defaultValue()).isEqualTo("http://ollama:11434");
        assertThat(findEntry("cvm.llm.embedding.ollama.model").defaultValue()).isEqualTo("nomic-embed-text");

        assertThat(findEntry("cvm.ai.reachability.enabled").defaultValue()).isEqualTo("false");
        assertThat(findEntry("cvm.ai.reachability.timeout-seconds").defaultValue()).isEqualTo("300");
        assertThat(findEntry("cvm.ai.reachability.binary").defaultValue()).isEqualTo("claude");

        assertThat(findEntry("cvm.ai.auto-assessment.enabled").defaultValue()).isEqualTo("false");
        assertThat(findEntry("cvm.ai.auto-assessment.top-k").defaultValue()).isEqualTo("5");
        assertThat(findEntry("cvm.ai.auto-assessment.min-rag-score").defaultValue()).isEqualTo("0.6");

        assertThat(findEntry("cvm.ai.anomaly.enabled").defaultValue()).isEqualTo("false");
        assertThat(findEntry("cvm.ai.anomaly.kev-epss-threshold").defaultValue()).isEqualTo("0.7");
        assertThat(findEntry("cvm.ai.anomaly.many-accept-risk-threshold").defaultValue()).isEqualTo("5");
        assertThat(findEntry("cvm.ai.anomaly.similar-rejection-threshold").defaultValue()).isEqualTo("0.9");
        assertThat(findEntry("cvm.ai.anomaly.use-llm-second-stage").defaultValue()).isEqualTo("false");
    }

    @Test
    @DisplayName("Boolean-Parameter haben Typ BOOLEAN, URLs haben Typ URL, Select-Parameter haben Optionen")
    void typen_sind_konsistent() {
        assertThat(findEntry("cvm.llm.enabled").type()).isEqualTo(SystemParameterType.BOOLEAN);
        assertThat(findEntry("cvm.ai.reachability.enabled").type()).isEqualTo(SystemParameterType.BOOLEAN);
        assertThat(findEntry("cvm.ai.anomaly.enabled").type()).isEqualTo(SystemParameterType.BOOLEAN);
        assertThat(findEntry("cvm.ai.copilot.enabled").type()).isEqualTo(SystemParameterType.BOOLEAN);
        assertThat(findEntry("cvm.ai.auto-assessment.enabled").type()).isEqualTo(SystemParameterType.BOOLEAN);

        assertThat(findEntry("cvm.llm.ollama.base-url").type()).isEqualTo(SystemParameterType.URL);
        assertThat(findEntry("cvm.llm.embedding.ollama.base-url").type()).isEqualTo(SystemParameterType.URL);

        SystemParameterCatalogEntry injection = findEntry("cvm.llm.injection.mode");
        assertThat(injection.type()).isEqualTo(SystemParameterType.SELECT);
        assertThat(injection.options()).isEqualTo("warn,block");

        assertThat(findEntry("cvm.ai.reachability.timeout-seconds").type())
                .isEqualTo(SystemParameterType.INTEGER);
        assertThat(findEntry("cvm.ai.anomaly.kev-epss-threshold").type())
                .isEqualTo(SystemParameterType.DECIMAL);
    }

    @Test
    @DisplayName("Kategorie-Konstanten decken die Block-A1-Kategorien ab")
    void kategorien_vorhanden() {
        Set<String> cats = SystemParameterCatalog.entries().stream()
                .map(SystemParameterCatalogEntry::category)
                .collect(Collectors.toSet());
        assertThat(cats).contains(
                SystemParameterCatalog.CATEGORY_AI_LLM,
                SystemParameterCatalog.CATEGORY_AI_REACHABILITY,
                SystemParameterCatalog.CATEGORY_RAG,
                SystemParameterCatalog.CATEGORY_ANOMALY,
                SystemParameterCatalog.CATEGORY_COPILOT);
    }

    private SystemParameterCatalogEntry findEntry(String key) {
        return SystemParameterCatalog.entries().stream()
                .filter(e -> e.paramKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Key fehlt im Katalog: " + key));
    }

    private Set<String> paramKeys() {
        return SystemParameterCatalog.entries().stream()
                .map(SystemParameterCatalogEntry::paramKey)
                .collect(Collectors.toSet());
    }
}
