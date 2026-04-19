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
    @DisplayName("Secrets (sensitive=true) tragen kein defaultValue und sind PASSWORD")
    void secrets_korrekt_konfiguriert() {
        // Iteration 66 (CVM-303): cvm.llm.claude.api-key ist jetzt
        // live-reloadable (restartRequired=false). Die drei verbleibenden
        // Secrets bleiben restartRequired, bis ihre Adapter migriert sind
        // (Iteration 67/68).
        String[] secretKeys = {
                "cvm.llm.claude.api-key",
                "cvm.feed.nvd.api-key",
                "cvm.feed.ghsa.api-key",
                "cvm.ai.fix-verification.github.token"
        };
        for (String key : secretKeys) {
            SystemParameterCatalogEntry entry = findEntry(key);
            assertThat(entry.sensitive()).as("sensitive fuer %s", key).isTrue();
            assertThat(entry.type()).as("type fuer %s", key)
                    .isEqualTo(SystemParameterType.PASSWORD);
            assertThat(entry.defaultValue()).as("defaultValue fuer %s", key).isNull();
        }
        // Iteration 68 (CVM-305): alle vier Secrets sind jetzt
        // live-reloadable; die Adapter lesen den Wert pro Call ueber
        // den SystemParameterResolver. Die AES-GCM-Verschluesselung
        // bleibt erhalten.
        for (String key : secretKeys) {
            assertThat(findEntry(key).restartRequired())
                    .as("Secret %s ist nach Iteration 68 live-reloadable", key)
                    .isFalse();
            assertThat(findEntry(key).hotReload())
                    .as("Secret %s ist nach Iteration 68 hotReload=true", key)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Nicht-Secret-Eintraege sind sensitive=false")
    void nicht_secrets_sind_nicht_sensitive() {
        long sensitiveCount = SystemParameterCatalog.entries().stream()
                .filter(SystemParameterCatalogEntry::sensitive)
                .count();
        // Vier Secrets (Iteration 45).
        assertThat(sensitiveCount).isEqualTo(4);
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

    @Test
    @DisplayName("Block A.2: ENRICHMENT, PIPELINE_GATE, MAIL, SCAN, SCHEDULER, SECURITY sind vollstaendig")
    void block_a2_vollstaendig() {
        Set<String> keys = paramKeys();
        assertThat(keys).contains(
                // ENRICHMENT
                "cvm.enrichment.osv.enabled",
                "cvm.enrichment.osv.batch-size",
                "cvm.enrichment.osv.timeout-ms",
                "cvm.enrichment.osv.retry-on-429",
                "cvm.enrichment.osv.max-retry-after-seconds",
                "cvm.feed.nvd.enabled",
                "cvm.feed.nvd.requests-per-window",
                "cvm.feed.nvd.window-seconds",
                "cvm.feed.ghsa.enabled",
                "cvm.feed.ghsa.requests-per-window",
                "cvm.feed.ghsa.window-seconds",
                "cvm.feed.kev.enabled",
                "cvm.feed.kev.requests-per-window",
                "cvm.feed.kev.window-seconds",
                "cvm.feed.epss.enabled",
                "cvm.feed.epss.requests-per-window",
                "cvm.feed.epss.window-seconds",
                // PIPELINE_GATE
                "cvm.pipeline.gate.per-minute",
                "cvm.pipeline.gate.post-mr-comment",
                // MAIL (Alerts)
                "cvm.alerts.mode",
                "cvm.alerts.from",
                "cvm.alerts.eskalation.t1-minutes",
                "cvm.alerts.eskalation.t2-minutes",
                // SCAN (Assessment + AI-Workloads)
                "cvm.assessment.default-valid-months",
                "cvm.ai.summary.min-delta",
                "cvm.ai.nl-query.result-limit",
                "cvm.ai.rule-extraction.enabled",
                "cvm.ai.rule-extraction.window-days",
                "cvm.ai.rule-extraction.cluster-cap",
                "cvm.ai.rule-extraction.override-review-threshold",
                "cvm.ai.fix-verification.enabled",
                "cvm.ai.fix-verification.full-text-commit-cap",
                "cvm.ai.fix-verification.cache-ttl-minutes",
                "cvm.ai.profile-assistant.enabled",
                "cvm.ai.profile-assistant.session-ttl-hours",
                "cvm.ai.profile-assist.cleanup-days",
                // SCHEDULER
                "cvm.scheduler.enabled",
                "cvm.assessment.expiry-cron",
                "cvm.ai.fix-verification.watchdog-cron",
                "cvm.ai.profile-assist.cleanup-cron",
                "cvm.ai.rule-extraction.cron",
                // SECURITY
                "cvm.security.cors.allowed-origins");
    }

    @Test
    @DisplayName("Block A.2 Defaults stimmen mit den @Value-Fallbacks und @ConfigurationProperties-Defaults ueberein")
    void block_a2_defaults_spiegeln_fallbacks() {
        // OSV (OsvProperties)
        assertThat(findEntry("cvm.enrichment.osv.enabled").defaultValue()).isEqualTo("false");
        assertThat(findEntry("cvm.enrichment.osv.batch-size").defaultValue()).isEqualTo("500");
        assertThat(findEntry("cvm.enrichment.osv.timeout-ms").defaultValue()).isEqualTo("15000");
        assertThat(findEntry("cvm.enrichment.osv.retry-on-429").defaultValue()).isEqualTo("true");
        assertThat(findEntry("cvm.enrichment.osv.max-retry-after-seconds").defaultValue()).isEqualTo("30");

        // Feeds (FeedProperties)
        assertThat(findEntry("cvm.feed.nvd.enabled").defaultValue()).isEqualTo("true");
        assertThat(findEntry("cvm.feed.nvd.requests-per-window").defaultValue()).isEqualTo("50");
        assertThat(findEntry("cvm.feed.nvd.window-seconds").defaultValue()).isEqualTo("30");
        assertThat(findEntry("cvm.feed.ghsa.enabled").defaultValue()).isEqualTo("false");
        assertThat(findEntry("cvm.feed.kev.enabled").defaultValue()).isEqualTo("true");
        assertThat(findEntry("cvm.feed.epss.enabled").defaultValue()).isEqualTo("true");

        // Pipeline-Gate
        assertThat(findEntry("cvm.pipeline.gate.per-minute").defaultValue()).isEqualTo("20");
        assertThat(findEntry("cvm.pipeline.gate.post-mr-comment").defaultValue()).isEqualTo("false");

        // Alerts
        assertThat(findEntry("cvm.alerts.mode").defaultValue()).isEqualTo("dry-run");
        assertThat(findEntry("cvm.alerts.from").defaultValue()).isEqualTo("cvm-alerts@ahs.local");
        assertThat(findEntry("cvm.alerts.eskalation.t1-minutes").defaultValue()).isEqualTo("120");
        assertThat(findEntry("cvm.alerts.eskalation.t2-minutes").defaultValue()).isEqualTo("360");

        // SCAN
        assertThat(findEntry("cvm.assessment.default-valid-months").defaultValue()).isEqualTo("12");
        assertThat(findEntry("cvm.ai.summary.min-delta").defaultValue()).isEqualTo("1");
        assertThat(findEntry("cvm.ai.nl-query.result-limit").defaultValue()).isEqualTo("100");
        assertThat(findEntry("cvm.ai.rule-extraction.window-days").defaultValue()).isEqualTo("180");
        assertThat(findEntry("cvm.ai.rule-extraction.cluster-cap").defaultValue()).isEqualTo("10");
        assertThat(findEntry("cvm.ai.rule-extraction.override-review-threshold").defaultValue()).isEqualTo("4");
        assertThat(findEntry("cvm.ai.fix-verification.full-text-commit-cap").defaultValue()).isEqualTo("50");
        assertThat(findEntry("cvm.ai.fix-verification.cache-ttl-minutes").defaultValue()).isEqualTo("1440");
        assertThat(findEntry("cvm.ai.profile-assistant.session-ttl-hours").defaultValue()).isEqualTo("24");
        assertThat(findEntry("cvm.ai.profile-assist.cleanup-days").defaultValue()).isEqualTo("7");

        // Scheduler-Crons
        assertThat(findEntry("cvm.scheduler.enabled").defaultValue()).isEqualTo("true");
        assertThat(findEntry("cvm.assessment.expiry-cron").defaultValue()).isEqualTo("0 0 3 * * *");
        assertThat(findEntry("cvm.ai.fix-verification.watchdog-cron").defaultValue()).isEqualTo("0 0 4 * * *");
        assertThat(findEntry("cvm.ai.profile-assist.cleanup-cron").defaultValue()).isEqualTo("0 15 2 * * *");
        assertThat(findEntry("cvm.ai.rule-extraction.cron").defaultValue()).isEqualTo("0 30 2 * * *");

        // Security
        assertThat(findEntry("cvm.security.cors.allowed-origins").defaultValue())
                .isEqualTo("http://localhost:4200");
    }

    @Test
    @DisplayName("Block A.2 Typen: Alerts-Mode ist SELECT mit drei Optionen, Cron ist STRING, EMAIL ist EMAIL")
    void block_a2_typen() {
        SystemParameterCatalogEntry mode = findEntry("cvm.alerts.mode");
        assertThat(mode.type()).isEqualTo(SystemParameterType.SELECT);
        assertThat(mode.options()).isEqualTo("dry-run,log,live");

        assertThat(findEntry("cvm.alerts.from").type()).isEqualTo(SystemParameterType.EMAIL);
        assertThat(findEntry("cvm.assessment.expiry-cron").type()).isEqualTo(SystemParameterType.STRING);
        assertThat(findEntry("cvm.scheduler.enabled").type()).isEqualTo(SystemParameterType.BOOLEAN);
        assertThat(findEntry("cvm.enrichment.osv.retry-on-429").type()).isEqualTo(SystemParameterType.BOOLEAN);
        assertThat(findEntry("cvm.pipeline.gate.per-minute").type()).isEqualTo(SystemParameterType.INTEGER);
    }

    @Test
    @DisplayName("Block A.2 Kategorien sind vollstaendig")
    void block_a2_kategorien() {
        Set<String> cats = SystemParameterCatalog.entries().stream()
                .map(SystemParameterCatalogEntry::category)
                .collect(Collectors.toSet());
        assertThat(cats).contains(
                SystemParameterCatalog.CATEGORY_ENRICHMENT,
                SystemParameterCatalog.CATEGORY_PIPELINE_GATE,
                SystemParameterCatalog.CATEGORY_MAIL,
                SystemParameterCatalog.CATEGORY_SCAN,
                SystemParameterCatalog.CATEGORY_SCHEDULER,
                SystemParameterCatalog.CATEGORY_SECURITY);
    }

    @Test
    @DisplayName("restartRequired ist fuer Keys gesetzt, die beim Boot in RestClient.Builder/Bucket4j/@Scheduled zementiert werden")
    void restart_required_markiert_richtige_keys() {
        // Muss restartRequired=true haben (RestClient.Builder, Bucket4j, @Scheduled):
        // Iteration 66 (CVM-303): Claude-Timeout, -Model und -ApiKey
        // wurden auf live-reloadable umgestellt und aus dieser Liste
        // entfernt; Version bleibt als RestClient-Header restartRequired.
        String[] mussRestart = {
                "cvm.llm.claude.version",
                "cvm.llm.ollama.base-url",
                "cvm.llm.ollama.model",
                "cvm.llm.embedding.ollama.base-url",
                "cvm.llm.embedding.ollama.model",
                "cvm.llm.openai.default-model",
                "cvm.llm.rate-limit.global-per-minute",
                "cvm.llm.rate-limit.tenant-per-minute",
                "cvm.pipeline.gate.per-minute",
                "cvm.security.cors.allowed-origins",
                "cvm.assessment.expiry-cron",
                "cvm.ai.fix-verification.watchdog-cron",
                "cvm.ai.profile-assist.cleanup-cron",
                "cvm.ai.rule-extraction.cron"
        };
        for (String key : mussRestart) {
            assertThat(findEntry(key).restartRequired())
                    .as("restartRequired fuer %s", key)
                    .isTrue();
        }

        // Muss restartRequired=false haben (Laufzeit-lesbar):
        String[] mussNichtRestart = {
                "cvm.llm.enabled",
                "cvm.llm.injection.mode",
                "cvm.llm.claude.base-url",
                "cvm.llm.claude.timeout-seconds",
                "cvm.llm.claude.model",
                "cvm.llm.claude.api-key",
                "cvm.feed.nvd.api-key",
                "cvm.feed.ghsa.api-key",
                "cvm.ai.fix-verification.github.token",
                "cvm.ai.reachability.enabled",
                "cvm.ai.reachability.timeout-seconds",
                "cvm.ai.auto-assessment.enabled",
                "cvm.ai.anomaly.enabled",
                "cvm.ai.copilot.enabled",
                "cvm.enrichment.osv.enabled",
                "cvm.feed.nvd.enabled",
                "cvm.alerts.mode",
                "cvm.alerts.from",
                "cvm.assessment.default-valid-months",
                "cvm.scheduler.enabled"
        };
        for (String key : mussNichtRestart) {
            assertThat(findEntry(key).restartRequired())
                    .as("restartRequired fuer %s", key)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("Keine Block-A.2-Keys werden fuer nicht-migrierbare Prefixes eingebunden")
    void nicht_migrieren_liste_respektiert() {
        Set<String> keys = paramKeys();
        // Spring-Infrastruktur bleibt in application.yaml.
        assertThat(keys).noneMatch(k -> k.startsWith("spring.")
                || k.startsWith("server.")
                || k.startsWith("management.")
                || k.startsWith("logging."));
        // Pricing und base-urls bleiben ebenfalls in application.yaml.
        assertThat(keys).noneMatch(k -> k.startsWith("cvm.llm.pricing."));
        assertThat(keys).doesNotContain("cvm.enrichment.osv.base-url");
        assertThat(keys).noneMatch(k -> k.matches("cvm\\.feed\\.[a-z]+\\.base-url"));
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
