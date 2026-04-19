package com.ahs.cvm.application.llmconfig;

import com.ahs.cvm.application.scan.SbomEncryption;
import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.application.tenant.TenantLookupService;
import com.ahs.cvm.persistence.llmconfig.LlmConfiguration;
import com.ahs.cvm.persistence.llmconfig.LlmConfigurationRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD-Service fuer {@link LlmConfiguration} (Iteration 34, CVM-78).
 *
 * <p>Schreibt und liest mandantenspezifische LLM-Zugaenge. Das Secret
 * wird ueber {@link SbomEncryption} AES-GCM-verschluesselt als
 * Base64-String persistiert. Views geben nie den Klartext zurueck.
 */
@Service
public class LlmConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(
            LlmConfigurationService.class);

    private final LlmConfigurationRepository repository;
    private final TenantLookupService tenantLookup;
    private final SbomEncryption encryption;
    private final LlmConnectionTester tester;

    public LlmConfigurationService(
            LlmConfigurationRepository repository,
            TenantLookupService tenantLookup,
            SbomEncryption encryption,
            LlmConnectionTester tester) {
        this.repository = repository;
        this.tenantLookup = tenantLookup;
        this.encryption = encryption;
        this.tester = tester;
    }

    @Transactional(readOnly = true)
    public List<LlmConfigurationView> listForCurrentTenant() {
        UUID tenantId = resolveTenantId();
        return repository.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public LlmConfigurationView findById(UUID id) {
        UUID tenantId = resolveTenantId();
        LlmConfiguration cfg = loadOwn(id, tenantId);
        return toView(cfg);
    }

    @Transactional(readOnly = true)
    public Optional<LlmConfigurationView> activeForCurrentTenant() {
        UUID tenantId = resolveTenantId();
        return repository.findByTenantIdAndActiveTrue(tenantId).map(this::toView);
    }

    @Transactional
    public LlmConfigurationView create(
            LlmConfigurationCommands.Create cmd, String actor) {
        if (cmd == null) {
            throw new IllegalArgumentException("Create-Kommando fehlt.");
        }
        validatePflicht(cmd.name(), cmd.provider(), cmd.model());
        String provider = ProviderDefaults.normalize(cmd.provider());
        ensureProviderKnown(provider);
        String baseUrl = resolveBaseUrl(provider, cmd.baseUrl());
        validateTemperature(cmd.temperature());
        validateMaxTokens(cmd.maxTokens());

        UUID tenantId = resolveTenantId();
        if (repository.findByTenantIdAndName(tenantId, cmd.name()).isPresent()) {
            throw new IllegalArgumentException(
                    "Eine Konfiguration mit dem Namen '" + cmd.name()
                            + "' existiert bereits fuer diesen Mandanten.");
        }

        LlmConfiguration entity = LlmConfiguration.builder()
                .tenantId(tenantId)
                .name(cmd.name().trim())
                .description(trim(cmd.description()))
                .provider(provider)
                .model(cmd.model().trim())
                .baseUrl(baseUrl)
                .secretRef(encryptIfPresent(cmd.secret()))
                .maxTokens(cmd.maxTokens())
                .temperature(cmd.temperature())
                .active(cmd.active())
                .updatedBy(actor)
                .build();

        if (cmd.active()) {
            repository.deaktiviereAndereAktive(tenantId, null);
        }

        LlmConfiguration gespeichert = repository.save(entity);
        log.info(
                "LLM-Konfiguration angelegt: id={}, name={}, provider={}, active={}, actor={}",
                gespeichert.getId(), gespeichert.getName(),
                gespeichert.getProvider(), gespeichert.isActive(), actor);
        return toView(gespeichert);
    }

    @Transactional
    public LlmConfigurationView update(
            UUID id, LlmConfigurationCommands.Update cmd, String actor) {
        if (cmd == null) {
            throw new IllegalArgumentException("Update-Kommando fehlt.");
        }
        UUID tenantId = resolveTenantId();
        LlmConfiguration cfg = loadOwn(id, tenantId);

        if (cmd.name() != null) {
            if (cmd.name().isBlank()) {
                throw new IllegalArgumentException("Name darf nicht leer sein.");
            }
            if (!cmd.name().equals(cfg.getName())) {
                repository.findByTenantIdAndName(tenantId, cmd.name())
                        .filter(other -> !other.getId().equals(cfg.getId()))
                        .ifPresent(other -> {
                            throw new IllegalArgumentException(
                                    "Name bereits vergeben: " + cmd.name());
                        });
                cfg.setName(cmd.name().trim());
            }
        }
        if (cmd.description() != null) {
            cfg.setDescription(trim(cmd.description()));
        }
        if (cmd.provider() != null) {
            String normalized = ProviderDefaults.normalize(cmd.provider());
            ensureProviderKnown(normalized);
            cfg.setProvider(normalized);
        }
        if (cmd.model() != null) {
            if (cmd.model().isBlank()) {
                throw new IllegalArgumentException("Modell darf nicht leer sein.");
            }
            cfg.setModel(cmd.model().trim());
        }
        if (cmd.baseUrl() != null) {
            cfg.setBaseUrl(resolveBaseUrl(cfg.getProvider(), cmd.baseUrl()));
        }
        if (cmd.secretClear()) {
            cfg.setSecretRef(null);
        } else if (cmd.secret() != null) {
            cfg.setSecretRef(encryptIfPresent(cmd.secret()));
        }
        if (cmd.maxTokens() != null) {
            validateMaxTokens(cmd.maxTokens());
            cfg.setMaxTokens(cmd.maxTokens());
        }
        if (cmd.temperature() != null) {
            validateTemperature(cmd.temperature());
            cfg.setTemperature(cmd.temperature());
        }
        if (cmd.active() != null) {
            if (cmd.active() && !cfg.isActive()) {
                repository.deaktiviereAndereAktive(tenantId, cfg.getId());
            }
            cfg.setActive(cmd.active());
        }

        // Azure verlangt auch nach Update eine baseUrl. Pruefen, falls
        // zwischendurch auf Azure umgestellt wurde.
        if (ProviderDefaults.requiresExplicitBaseUrl(cfg.getProvider())
                && (cfg.getBaseUrl() == null || cfg.getBaseUrl().isBlank())) {
            throw new IllegalArgumentException(
                    "Provider 'azure' verlangt eine explizite baseUrl.");
        }

        cfg.setUpdatedBy(actor);
        LlmConfiguration gespeichert = repository.save(cfg);
        log.info(
                "LLM-Konfiguration aktualisiert: id={}, provider={}, active={}, actor={}",
                gespeichert.getId(), gespeichert.getProvider(),
                gespeichert.isActive(), actor);
        return toView(gespeichert);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = resolveTenantId();
        LlmConfiguration cfg = loadOwn(id, tenantId);
        repository.delete(cfg);
        log.info("LLM-Konfiguration geloescht: id={}, tenantId={}", id, tenantId);
    }

    /**
     * Fuehrt einen Verbindungstest gegen einen LLM-Provider aus.
     *
     * <p>Wenn der uebergebene Command eine {@code id} enthaelt, werden
     * fehlende Felder (baseUrl, model, apiKey) aus der gespeicherten
     * Konfiguration ergaenzt - das Secret wird dabei entschluesselt,
     * verlaesst aber den Prozess nicht. Ohne id ist der Test rein
     * ad-hoc gegen die im Command mitgegebenen Werte.
     *
     * <p>Zu keinem Zeitpunkt wird ein Audit-Eintrag erzeugt: ein
     * Verbindungstest ist kein fachlicher LLM-Call.
     */
    @Transactional(readOnly = true)
    public LlmConfigurationTestResult testConnection(
            LlmConfigurationTestCommand cmd) {
        if (cmd == null) {
            throw new IllegalArgumentException("Test-Kommando fehlt.");
        }
        String provider = cmd.provider();
        String model = cmd.model();
        String baseUrl = cmd.baseUrl();
        String apiKey = cmd.apiKey();
        if (cmd.id() != null) {
            UUID tenantId = resolveTenantId();
            LlmConfiguration cfg = loadOwn(cmd.id(), tenantId);
            if (provider == null || provider.isBlank()) provider = cfg.getProvider();
            if (model == null || model.isBlank()) model = cfg.getModel();
            if (baseUrl == null || baseUrl.isBlank()) baseUrl = cfg.getBaseUrl();
            if (apiKey == null || apiKey.isBlank()) {
                apiKey = decryptIfPresent(cfg.getSecretRef()).orElse(null);
            }
        }
        return tester.test(new LlmConfigurationTestCommand(
                cmd.id(), provider, model, baseUrl, apiKey));
    }

    /**
     * Fuer den LlmGateway-Code: liefert den entschluesselten API-Key,
     * wenn vorhanden. Diese Methode ist bewusst <em>nicht</em> ueber
     * einen HTTP-Endpoint erreichbar - Klartext verlaesst den
     * Backend-Prozess nie.
     */
    @Transactional(readOnly = true)
    public Optional<String> resolveSecret(UUID id) {
        UUID tenantId = resolveTenantId();
        LlmConfiguration cfg = loadOwn(id, tenantId);
        return decryptIfPresent(cfg.getSecretRef());
    }

    // --- Helper -------------------------------------------------------------

    private LlmConfiguration loadOwn(UUID id, UUID tenantId) {
        LlmConfiguration cfg = repository.findById(id).orElseThrow(
                () -> new LlmConfigurationNotFoundException(id));
        if (!cfg.getTenantId().equals(tenantId)) {
            // Tenant-Isolation: nie eine fremde Konfig zurueckgeben.
            throw new LlmConfigurationNotFoundException(id);
        }
        return cfg;
    }

    private UUID resolveTenantId() {
        return TenantContext.current()
                .orElseGet(() -> tenantLookup.findDefaultTenantId()
                        .orElseThrow(() -> new IllegalStateException(
                                "Kein Default-Mandant gefunden.")));
    }

    private static void validatePflicht(
            String name, String provider, String model) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name fehlt.");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Name zu lang (max 255).");
        }
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Provider fehlt.");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Model fehlt.");
        }
        if (model.length() > 255) {
            throw new IllegalArgumentException("Model zu lang (max 255).");
        }
    }

    private static void ensureProviderKnown(String provider) {
        if (!ProviderDefaults.isKnown(provider)) {
            throw new IllegalArgumentException(
                    "Unbekannter Provider '" + provider
                            + "'. Erlaubt: " + ProviderDefaults.PROVIDERS);
        }
    }

    private static String resolveBaseUrl(String provider, String raw) {
        String value = raw == null ? null : raw.trim();
        if (value != null && !value.isEmpty()) {
            if (value.length() > 2048) {
                throw new IllegalArgumentException("baseUrl zu lang (max 2048).");
            }
            return value;
        }
        if (ProviderDefaults.requiresExplicitBaseUrl(provider)) {
            throw new IllegalArgumentException(
                    "Provider 'azure' verlangt eine explizite baseUrl.");
        }
        return ProviderDefaults.defaultBaseUrl(provider).orElse(null);
    }

    private static void validateTemperature(java.math.BigDecimal t) {
        if (t == null) {
            return;
        }
        if (t.doubleValue() < 0.0 || t.doubleValue() > 1.0) {
            throw new IllegalArgumentException(
                    "temperature muss im Bereich [0.0, 1.0] liegen.");
        }
    }

    private static void validateMaxTokens(Integer value) {
        if (value == null) {
            return;
        }
        if (value < 1) {
            throw new IllegalArgumentException("maxTokens muss > 0 sein.");
        }
    }

    private static String trim(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private String encryptIfPresent(String klartext) {
        if (klartext == null) {
            return null;
        }
        String t = klartext.trim();
        if (t.isEmpty()) {
            return null;
        }
        byte[] cipher = encryption.encrypt(t.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(cipher);
    }

    private Optional<String> decryptIfPresent(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return Optional.empty();
        }
        byte[] cipher = Base64.getDecoder().decode(ciphertext);
        byte[] klar = encryption.decrypt(cipher);
        return Optional.of(new String(klar, StandardCharsets.UTF_8));
    }

    private LlmConfigurationView toView(LlmConfiguration cfg) {
        boolean secretSet = cfg.getSecretRef() != null
                && !cfg.getSecretRef().isBlank();
        String hint = null;
        if (secretSet) {
            Optional<String> klar = decryptIfPresent(cfg.getSecretRef());
            hint = klar
                    .map(k -> k.length() <= 4
                            ? "****"
                            : "****" + k.substring(k.length() - 4))
                    .orElse(null);
        }
        return new LlmConfigurationView(
                cfg.getId(),
                cfg.getTenantId(),
                cfg.getName(),
                cfg.getDescription(),
                cfg.getProvider(),
                cfg.getModel(),
                cfg.getBaseUrl(),
                secretSet,
                hint,
                cfg.getMaxTokens(),
                cfg.getTemperature(),
                cfg.isActive(),
                cfg.getCreatedAt(),
                cfg.getUpdatedAt(),
                cfg.getUpdatedBy());
    }

    /** 404-Marker, damit der ExceptionHandler ein sauberes Mapping hat. */
    public static final class LlmConfigurationNotFoundException
            extends RuntimeException {
        public LlmConfigurationNotFoundException(UUID id) {
            super("LLM-Konfiguration " + id + " nicht gefunden.");
        }
    }
}
