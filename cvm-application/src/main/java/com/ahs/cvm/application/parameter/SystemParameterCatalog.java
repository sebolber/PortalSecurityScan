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
    public static final String CATEGORY_ENRICHMENT = "ENRICHMENT";
    public static final String CATEGORY_PIPELINE_GATE = "PIPELINE_GATE";
    public static final String CATEGORY_MAIL = "MAIL";
    public static final String CATEGORY_SCAN = "SCAN";
    public static final String CATEGORY_SCHEDULER = "SCHEDULER";
    public static final String CATEGORY_SECURITY = "SECURITY";

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
        addEnrichment(list);
        addPipelineGate(list);
        addMail(list);
        addScan(list);
        addScheduler(list);
        addSecurity(list);
        addSecrets(list);
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
                "Bucket4j wird beim Boot gebaut - Aenderung erfordert Neustart.",
                CATEGORY_AI_LLM, "rate-limit",
                SystemParameterType.INTEGER, "120", true, null, null, "calls/min", false, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.rate-limit.tenant-per-minute",
                "Mandanten-Rate-Limit (pro Minute)",
                "Maximale Anzahl LLM-Calls pro Mandant und Minute.",
                "Bucket4j wird beim Boot gebaut - Aenderung erfordert Neustart.",
                CATEGORY_AI_LLM, "rate-limit",
                SystemParameterType.INTEGER, "30", true, null, null, "calls/min", false, false, true, true));

        // Claude-Fallback-Adapter.
        // Iteration 66 (CVM-303): base-url, timeout-seconds, model und
        // api-key werden jetzt pro Call ueber den
        // SystemParameterResolver aufgeloest und sind damit live
        // umschaltbar. anthropic-version bleibt im RestClient-Header
        // hart verdrahtet und erfordert weiterhin einen Neustart.
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.claude.base-url",
                "Claude Basis-URL",
                "Basis-URL fuer den Claude-Fallback-Adapter (z.B. Proxy oder Mandanten-Endpunkt).",
                "Wird pro Call ausgelesen; RestClient wird lazy rebuilt. Kein Neustart noetig.",
                CATEGORY_AI_LLM, "claude",
                SystemParameterType.STRING, "https://api.anthropic.com", true, null, null, null, false, true, true, false));
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.claude.version",
                "Claude API-Version",
                "Wert fuer den Header anthropic-version im Claude-Adapter.",
                "Header wird beim Boot in den RestClient hinterlegt - Neustart noetig.",
                CATEGORY_AI_LLM, "claude",
                SystemParameterType.STRING, "2023-06-01", true, null, null, null, false, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.claude.timeout-seconds",
                "Claude HTTP-Timeout",
                "Maximale Wartezeit fuer einen einzelnen Claude-API-Call.",
                "Wird pro Call ausgelesen; RestClient wird lazy rebuilt. Kein Neustart noetig.",
                CATEGORY_AI_LLM, "claude",
                SystemParameterType.INTEGER, "30", true, null, null, "Sekunden", false, true, true, false));
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.claude.model",
                "Claude-Fallback-Modell",
                "Modell-Bezeichner fuer den Claude-Adapter, wenn kein Profil existiert.",
                "Wird pro Call ausgelesen. Kein Neustart noetig.",
                CATEGORY_AI_LLM, "claude",
                SystemParameterType.STRING, "claude-sonnet-4-6", true, null, null, null, false, true, true, false));

        // Ollama-Adapter
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.ollama.base-url",
                "Ollama-Basis-URL",
                "HTTP-Basis-URL des Ollama-Servers (on-prem).",
                "Base-URL wird beim Boot in den RestClient hinterlegt - Neustart noetig.",
                CATEGORY_AI_LLM, "ollama",
                SystemParameterType.URL, "http://ollama:11434", true, null, null, null, false, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.ollama.model",
                "Ollama-Fallback-Modell",
                "Modell-Bezeichner fuer den Ollama-Adapter, wenn kein Profil existiert.",
                "Default wird beim Boot gelesen - Neustart noetig.",
                CATEGORY_AI_LLM, "ollama",
                SystemParameterType.STRING, "llama3.1:8b-instruct", true, null, null, null, false, false, true, true));

        // OpenAI-kompatibler Adapter (nur Default-Modell, kein Key)
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.openai.default-model",
                "OpenAI-Fallback-Modell",
                "Modell-Bezeichner fuer den OpenAI-kompatiblen Adapter.",
                "Default wird beim Boot gelesen - Neustart noetig.",
                CATEGORY_AI_LLM, "openai",
                SystemParameterType.STRING, "gpt-4o-mini", true, null, null, null, false, false, true, true));

        // Embedding-Adapter
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.embedding.ollama.base-url",
                "Embedding-Ollama-Basis-URL",
                "HTTP-Basis-URL des Ollama-Servers fuer Embeddings (RAG).",
                "Base-URL wird beim Boot in den RestClient hinterlegt - Neustart noetig.",
                CATEGORY_AI_LLM, "embedding",
                SystemParameterType.URL, "http://ollama:11434", true, null, null, null, false, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.embedding.ollama.model",
                "Embedding-Modell",
                "Modell-Bezeichner fuer Embeddings (RAG).",
                "Modell wird beim Boot gelesen - Neustart noetig.",
                CATEGORY_AI_LLM, "embedding",
                SystemParameterType.STRING, "nomic-embed-text", true, null, null, null, false, false, true, true));
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
        // Iteration 70 (CVM-307): Auto-Trigger fuer Reachability bei
        // niedriger AI-Confidence. Beide Eintraege sind live-reloadable.
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.reachability.auto-trigger-threshold",
                "Auto-Trigger Confidence-Schwelle",
                "Confidence-Grenzwert, unterhalb dessen ein AI-Vorschlag automatisch "
                        + "einen Reachability-Lauf ausloest. Wert zwischen 0 und 1.",
                "Wird pro Event gelesen. Kein Neustart noetig.",
                CATEGORY_AI_REACHABILITY, "auto-trigger",
                SystemParameterType.DECIMAL, "0.6", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.reachability.auto-trigger-cooldown-minutes",
                "Auto-Trigger Cooldown",
                "Mindestabstand zwischen zwei Auto-Triggers fuer dasselbe "
                        + "(ProduktVersion, CVE)-Paar.",
                "In-Memory-Rate-Limit. Kein Neustart noetig.",
                CATEGORY_AI_REACHABILITY, "auto-trigger",
                SystemParameterType.INTEGER, "60", true, null, null, "Minuten", false, true, true));
        // Iteration 71 (CVM-308): JGit-Adapter.
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.reachability.git.cache-dir",
                "Git-Checkout Cache-Verzeichnis",
                "Pfad, unter dem geklonte Repos pro Commit gecached werden.",
                "Wird beim Boot vom JGitGitCheckoutAdapter angelegt. Aenderung erfordert Neustart.",
                CATEGORY_AI_REACHABILITY, "git",
                SystemParameterType.STRING, null, false, null, null, null, false, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.reachability.git.cache-ttl-hours",
                "Git-Checkout Cache-TTL",
                "Lebensdauer eines Cache-Eintrags, nach der er vom Cleanup-Job entfernt wird.",
                "Wird pro Cleanup-Lauf gelesen. Kein Neustart noetig.",
                CATEGORY_AI_REACHABILITY, "git",
                SystemParameterType.INTEGER, "72", true, null, null, "Stunden", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.reachability.git.https-token",
                "Git HTTPS-Token",
                "Personal Access Token fuer Git-Server, die HTTPS-Auth "
                        + "erwarten (optional, leer = anonym).",
                "Wird AES-GCM-verschluesselt gespeichert und pro Clone gelesen.",
                CATEGORY_AI_REACHABILITY, "git",
                SystemParameterType.PASSWORD, null, false, null, null, null, true, true, true, false));
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

    private static void addEnrichment(List<SystemParameterCatalogEntry> list) {
        // OSV (base-url bleibt in application.yaml, siehe Nicht-migrieren-Liste)
        list.add(new SystemParameterCatalogEntry(
                "cvm.enrichment.osv.enabled",
                "OSV-Anreicherung aktiv",
                "Feature-Flag fuer die OSV-Anreicherung beim Scan-Ingest (Iteration 33).",
                "Bei false erzeugt der Adapter keinen Outbound-Traffic.",
                CATEGORY_ENRICHMENT, "osv",
                SystemParameterType.BOOLEAN, "false", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.enrichment.osv.batch-size",
                "OSV-Batch-Groesse",
                "Anzahl Komponenten pro OSV-Batch-Request (max. 1000).",
                null,
                CATEGORY_ENRICHMENT, "osv",
                SystemParameterType.INTEGER, "500", true, null, null, "Komponenten", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.enrichment.osv.timeout-ms",
                "OSV-HTTP-Timeout",
                "Maximale Wartezeit pro OSV-Aufruf.",
                null,
                CATEGORY_ENRICHMENT, "osv",
                SystemParameterType.INTEGER, "15000", true, null, null, "Millisekunden", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.enrichment.osv.retry-on-429",
                "OSV-Retry bei HTTP 429",
                "Wenn true, wird bei einer 429-Antwort einmalig nach Retry-After gewartet und wiederholt.",
                null,
                CATEGORY_ENRICHMENT, "osv",
                SystemParameterType.BOOLEAN, "true", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.enrichment.osv.max-retry-after-seconds",
                "OSV-Retry-After-Obergrenze",
                "Obergrenze fuer die im Retry-After-Header angegebene Wartezeit.",
                null,
                CATEGORY_ENRICHMENT, "osv",
                SystemParameterType.INTEGER, "30", true, null, null, "Sekunden", false, true, true));
        // Iteration 72 (CVM-309): File-basierter OSV-Mirror fuer
        // air-gapped Setups. Werden beim Boot gelesen, daher restartRequired.
        list.add(new SystemParameterCatalogEntry(
                "cvm.enrichment.osv.mirror.enabled",
                "OSV-Mirror aktiv",
                "Wenn true, liest ein lokales JSONL-File als OSV-Quelle; "
                        + "verdraengt den Netz-basierten Adapter.",
                "Schaltung beim Boot; Aenderung erfordert Neustart.",
                CATEGORY_ENRICHMENT, "osv-mirror",
                SystemParameterType.BOOLEAN, "false", true, null, null, null, false, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.enrichment.osv.mirror.file",
                "OSV-Mirror JSONL-Pfad",
                "Absoluter Pfad der JSONL-Datei mit OSV-Advisories "
                        + "(eine Advisory pro Zeile).",
                "Datei wird beim Boot gelesen; Aenderung erfordert Neustart.",
                CATEGORY_ENRICHMENT, "osv-mirror",
                SystemParameterType.STRING, null, false, null, null, null, false, false, true, true));

        addFeed(list, "nvd", "NVD", "true", "50", "30");
        addFeed(list, "ghsa", "GHSA", "false", "30", "60");
        addFeed(list, "kev", "KEV", "true", "60", "60");
        addFeed(list, "epss", "EPSS", "true", "60", "60");
    }

    private static void addFeed(
            List<SystemParameterCatalogEntry> list,
            String slug,
            String anzeigeName,
            String enabledDefault,
            String requestsDefault,
            String windowDefault) {
        list.add(new SystemParameterCatalogEntry(
                "cvm.feed." + slug + ".enabled",
                anzeigeName + "-Feed aktiv",
                "Feature-Flag fuer den " + anzeigeName + "-Feed (CVE-Anreicherung).",
                null,
                CATEGORY_ENRICHMENT, slug,
                SystemParameterType.BOOLEAN, enabledDefault, true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.feed." + slug + ".requests-per-window",
                anzeigeName + "-Requests pro Fenster",
                "Maximale Anzahl Requests innerhalb des Rate-Limit-Fensters.",
                null,
                CATEGORY_ENRICHMENT, slug,
                SystemParameterType.INTEGER, requestsDefault, true, null, null, "Requests", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.feed." + slug + ".window-seconds",
                anzeigeName + "-Fenster",
                "Groesse des Rate-Limit-Fensters.",
                null,
                CATEGORY_ENRICHMENT, slug,
                SystemParameterType.INTEGER, windowDefault, true, null, null, "Sekunden", false, true, true));
    }

    private static void addPipelineGate(List<SystemParameterCatalogEntry> list) {
        list.add(new SystemParameterCatalogEntry(
                "cvm.pipeline.gate.per-minute",
                "Pipeline-Gate Rate-Limit",
                "Maximale Anzahl Gate-Checks pro Minute (Bucket4j).",
                "Bucket4j wird beim Boot gebaut - Aenderung erfordert Neustart.",
                CATEGORY_PIPELINE_GATE, "rate-limit",
                SystemParameterType.INTEGER, "20", true, null, null, "calls/min", false, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.pipeline.gate.post-mr-comment",
                "Pipeline-Gate MR-Kommentar",
                "Wenn true, postet der Pipeline-Gate nach einer Bewertung einen MR-Kommentar.",
                null,
                CATEGORY_PIPELINE_GATE, "git",
                SystemParameterType.BOOLEAN, "false", true, null, null, null, false, true, true));
    }

    private static void addMail(List<SystemParameterCatalogEntry> list) {
        list.add(new SystemParameterCatalogEntry(
                "cvm.alerts.mode",
                "Alert-Modus",
                "Betriebsart des Alert-Versands: dry-run, log, live.",
                "dry-run verschickt keine Mails. log schreibt ins Audit-Log. live versendet tatsaechlich.",
                CATEGORY_MAIL, "alerts",
                SystemParameterType.SELECT, "dry-run", true, null, "dry-run,log,live", null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.alerts.from",
                "Alert-From-Adresse",
                "Absender-Adresse fuer Alert-Mails.",
                null,
                CATEGORY_MAIL, "alerts",
                SystemParameterType.EMAIL, "cvm-alerts@ahs.local", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.alerts.eskalation.t1-minutes",
                "Eskalationsstufe T1",
                "Wartezeit bis zur ersten Eskalation.",
                null,
                CATEGORY_MAIL, "eskalation",
                SystemParameterType.INTEGER, "120", true, null, null, "Minuten", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.alerts.eskalation.t2-minutes",
                "Eskalationsstufe T2",
                "Wartezeit bis zur zweiten Eskalation.",
                null,
                CATEGORY_MAIL, "eskalation",
                SystemParameterType.INTEGER, "360", true, null, null, "Minuten", false, true, true));
    }

    private static void addScan(List<SystemParameterCatalogEntry> list) {
        list.add(new SystemParameterCatalogEntry(
                "cvm.assessment.default-valid-months",
                "Assessment-Standardlaufzeit",
                "Default-Lebenszeit fuer ein Assessment in Monaten.",
                null,
                CATEGORY_SCAN, "assessment",
                SystemParameterType.INTEGER, "12", true, null, null, "Monate", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.summary.min-delta",
                "Delta-Summary Mindestanzahl",
                "Minimale Anzahl geaenderter Findings, bevor eine Delta-Summary erzeugt wird.",
                null,
                CATEGORY_SCAN, "summary",
                SystemParameterType.INTEGER, "1", true, null, null, "Findings", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.nl-query.result-limit",
                "NL-Query Ergebnis-Limit",
                "Maximale Anzahl Zeilen, die eine natuerlich-sprachliche Query zurueckliefert.",
                null,
                CATEGORY_SCAN, "nl-query",
                SystemParameterType.INTEGER, "100", true, null, null, "Zeilen", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.rule-extraction.enabled",
                "Regel-Extraktion aktiv",
                "Feature-Flag fuer die KI-gestuetzte Regel-Extraktion (Iteration 17).",
                null,
                CATEGORY_SCAN, "rule-extraction",
                SystemParameterType.BOOLEAN, "false", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.rule-extraction.window-days",
                "Regel-Extraktion Fenster",
                "Zeitfenster fuer die Analyse historischer Assessments.",
                null,
                CATEGORY_SCAN, "rule-extraction",
                SystemParameterType.INTEGER, "180", true, null, null, "Tage", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.rule-extraction.cluster-cap",
                "Regel-Extraktion Cluster-Cap",
                "Maximale Anzahl Regel-Kandidaten pro Lauf.",
                null,
                CATEGORY_SCAN, "rule-extraction",
                SystemParameterType.INTEGER, "10", true, null, null, "Kandidaten", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.rule-extraction.override-review-threshold",
                "Regel-Extraktion Review-Schwelle",
                "Minimale Override-Anzahl, ab der ein Kandidat ein Review ausloest.",
                null,
                CATEGORY_SCAN, "rule-extraction",
                SystemParameterType.INTEGER, "4", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.fix-verification.enabled",
                "Fix-Verifikation aktiv",
                "Feature-Flag fuer die Fix-Verifikation (Iteration 16).",
                null,
                CATEGORY_SCAN, "fix-verification",
                SystemParameterType.BOOLEAN, "false", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.fix-verification.full-text-commit-cap",
                "Fix-Verifikation Commit-Cap",
                "Maximale Anzahl Commits, die im Volltext abgefragt werden.",
                null,
                CATEGORY_SCAN, "fix-verification",
                SystemParameterType.INTEGER, "50", true, null, null, "Commits", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.fix-verification.cache-ttl-minutes",
                "Fix-Verifikation Cache-TTL",
                "Gueltigkeitsdauer des Ergebnis-Caches.",
                null,
                CATEGORY_SCAN, "fix-verification",
                SystemParameterType.INTEGER, "1440", true, null, null, "Minuten", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.profile-assistant.enabled",
                "Profil-Assistent aktiv",
                "Feature-Flag fuer den Profil-Assistenten (Iteration 18).",
                null,
                CATEGORY_SCAN, "profile-assistant",
                SystemParameterType.BOOLEAN, "false", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.profile-assistant.session-ttl-hours",
                "Profil-Assistent Session-TTL",
                "Gueltigkeitsdauer einer Assistenten-Session.",
                null,
                CATEGORY_SCAN, "profile-assistant",
                SystemParameterType.INTEGER, "24", true, null, null, "Stunden", false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.profile-assist.cleanup-days",
                "Profil-Assistent Cleanup-Alter",
                "Alter ab dem abgeschlossene Assistenten-Sessions geloescht werden.",
                null,
                CATEGORY_SCAN, "profile-assistant",
                SystemParameterType.INTEGER, "7", true, null, null, "Tage", false, true, true));
    }

    private static void addScheduler(List<SystemParameterCatalogEntry> list) {
        list.add(new SystemParameterCatalogEntry(
                "cvm.scheduler.enabled",
                "Scheduler global aktiv",
                "Globaler Schalter fuer alle Hintergrund-Jobs (Cron).",
                "Testprofile setzen den Wert auf false; Produktion laesst ihn auf true.",
                CATEGORY_SCHEDULER, "global",
                SystemParameterType.BOOLEAN, "true", true, null, null, null, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.assessment.expiry-cron",
                "Cron: Assessment-Ablauf",
                "Cron-Ausdruck fuer den AssessmentExpiryJob.",
                "Spring-Cron-Format: sek min std tag mon dow. Neustart noetig.",
                CATEGORY_SCHEDULER, "cron",
                SystemParameterType.STRING, "0 0 3 * * *", true, null, null, null, false, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.fix-verification.watchdog-cron",
                "Cron: Fix-Verifikation Watchdog",
                "Cron-Ausdruck fuer den OpenFixWatchdog.",
                "Neustart noetig.",
                CATEGORY_SCHEDULER, "cron",
                SystemParameterType.STRING, "0 0 4 * * *", true, null, null, null, false, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.profile-assist.cleanup-cron",
                "Cron: Profil-Assistent Cleanup",
                "Cron-Ausdruck fuer den ProfileAssistSessionCleanupJob.",
                "Neustart noetig.",
                CATEGORY_SCHEDULER, "cron",
                SystemParameterType.STRING, "0 15 2 * * *", true, null, null, null, false, false, true, true));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.rule-extraction.cron",
                "Cron: Regel-Extraktion",
                "Cron-Ausdruck fuer den RuleExtractionJob.",
                "Neustart noetig.",
                CATEGORY_SCHEDULER, "cron",
                SystemParameterType.STRING, "0 30 2 * * *", true, null, null, null, false, false, true, true));
    }

    private static void addSecurity(List<SystemParameterCatalogEntry> list) {
        list.add(new SystemParameterCatalogEntry(
                "cvm.security.cors.allowed-origins",
                "CORS Allowed Origins",
                "Komma-separierte Liste erlaubter CORS-Origins.",
                "Wirkt nur beim Boot - Aenderung erfordert Neustart.",
                CATEGORY_SECURITY, "cors",
                SystemParameterType.STRING, "http://localhost:4200", true, null, null, null, false, false, true, true));
    }

    private static void addSecrets(List<SystemParameterCatalogEntry> list) {
        // Iteration 45: sensitive=true + restartRequired=true.
        // defaultValue absichtlich null - Bootstrap seedet keinen Wert.
        // cvm.encryption.sbom-secret ist der Master-Key fuer SBOM-AES-GCM
        // und bleibt bewusst in application.yaml (Henne-Ei).
        list.add(new SystemParameterCatalogEntry(
                "cvm.llm.claude.api-key",
                "Anthropic API-Key",
                "API-Key fuer den Claude-Fallback-Adapter.",
                "Iteration 66: Wird AES-GCM-verschluesselt gespeichert und pro Call ausgelesen. Kein Neustart noetig.",
                CATEGORY_AI_LLM, "claude",
                SystemParameterType.PASSWORD, null, false, null, null, null, true, true, true, false));
        list.add(new SystemParameterCatalogEntry(
                "cvm.feed.nvd.api-key",
                "NVD API-Key",
                "API-Key fuer den NVD-Feed. Optional, erhoeht das Rate-Limit.",
                "Iteration 67: Wird AES-GCM-verschluesselt gespeichert und pro Call ausgelesen. Kein Neustart noetig.",
                CATEGORY_ENRICHMENT, "nvd",
                SystemParameterType.PASSWORD, null, false, null, null, null, true, true, true, false));
        list.add(new SystemParameterCatalogEntry(
                "cvm.feed.ghsa.api-key",
                "GitHub GHSA-Token",
                "Personal Access Token fuer die GHSA-GraphQL-API.",
                "Iteration 67: Wird AES-GCM-verschluesselt gespeichert und pro Call ausgelesen. Kein Neustart noetig.",
                CATEGORY_ENRICHMENT, "ghsa",
                SystemParameterType.PASSWORD, null, false, null, null, null, true, true, true, false));
        list.add(new SystemParameterCatalogEntry(
                "cvm.ai.fix-verification.github.token",
                "Fix-Verifikation GitHub-Token",
                "Personal Access Token fuer die GitHub-REST-API (Fix-Verifikation).",
                "Iteration 68: Wird AES-GCM-verschluesselt gespeichert und pro Call ausgelesen. Kein Neustart noetig.",
                CATEGORY_SCAN, "fix-verification",
                SystemParameterType.PASSWORD, null, false, null, null, null, true, true, true, false));
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
