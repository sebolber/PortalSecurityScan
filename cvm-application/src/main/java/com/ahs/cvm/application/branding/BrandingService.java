package com.ahs.cvm.application.branding;

import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.application.tenant.TenantLookupService;
import com.ahs.cvm.persistence.branding.BrandingConfig;
import com.ahs.cvm.persistence.branding.BrandingConfigHistory;
import com.ahs.cvm.persistence.branding.BrandingConfigHistoryRepository;
import com.ahs.cvm.persistence.branding.BrandingConfigRepository;
import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lese-/Schreib-Service fuer Mandanten-Branding (Iterationen 27 + 31, CVM-61/72).
 *
 * <p>{@link #loadForCurrentTenant()} liefert das Branding des aktuellen
 * (via {@link TenantContext}) oder - wenn keiner gesetzt - des
 * Default-Mandanten. Das Ergebnis enthaelt immer valide Werte: fehlt
 * ein Datensatz, greift {@link BrandingDefaults} mit der adesso-CI-
 * Vorbelegung.
 *
 * <p>{@link #updateForCurrentTenant(BrandingUpdateCommand, String)}
 * erzwingt WCAG-AA-Kontrast und optimistisches Locking und schreibt
 * den alten Stand vor dem Speichern in {@link BrandingConfigHistory}.
 *
 * <p>{@link #rollbackForCurrentTenant(int, String)} laedt einen
 * History-Snapshot und stellt ihn als neue Version wieder her -
 * wiederum mit Kontrast-Check und History-Eintrag, damit auch
 * Rollbacks auditierbar bleiben.
 */
@Service
public class BrandingService {

    private static final Logger LOG = LoggerFactory.getLogger(BrandingService.class);

    private final BrandingConfigRepository repository;
    private final BrandingConfigHistoryRepository historyRepository;
    private final TenantLookupService tenantLookup;

    public BrandingService(
            BrandingConfigRepository repository,
            BrandingConfigHistoryRepository historyRepository,
            TenantLookupService tenantLookup) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.tenantLookup = tenantLookup;
    }

    @Transactional(readOnly = true)
    public BrandingView loadForCurrentTenant() {
        UUID tenantId = resolveTenantId();
        return repository
                .findByTenantId(tenantId)
                .map(BrandingService::toView)
                .orElseGet(BrandingDefaults::view);
    }

    @Transactional
    public BrandingView updateForCurrentTenant(
            BrandingUpdateCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("Update-Kommando fehlt.");
        }
        validateColors(command);
        UUID tenantId = resolveTenantId();

        BrandingConfig config = repository
                .findByTenantId(tenantId)
                .orElseGet(() -> BrandingConfig.builder()
                        .tenantId(tenantId)
                        .primaryColor(BrandingDefaults.PRIMARY)
                        .primaryContrastColor(BrandingDefaults.PRIMARY_CONTRAST)
                        .fontFamilyName(BrandingDefaults.FONT)
                        .version(0)
                        .updatedBy(actor)
                        .updatedAt(Instant.now())
                        .build());

        if (config.getVersion() != null
                && command.expectedVersion() != config.getVersion()) {
            throw new OptimisticLockException(
                    "Branding wurde inzwischen anderweitig aktualisiert "
                            + "(erwartet v" + command.expectedVersion()
                            + ", aktuell v" + config.getVersion() + ").");
        }

        if (config.getVersion() != null && config.getVersion() > 0) {
            historyRepository.save(snapshotFromConfig(config, actor));
        }

        config.setPrimaryColor(trim(command.primaryColor()));
        config.setPrimaryContrastColor(trim(command.primaryContrastColor()));
        config.setAccentColor(trim(command.accentColor()));
        config.setFontFamilyName(trim(command.fontFamilyName()));
        config.setFontFamilyMonoName(trim(command.fontFamilyMonoName()));
        config.setAppTitle(trim(command.appTitle()));
        config.setLogoUrl(trim(command.logoUrl()));
        config.setLogoAltText(trim(command.logoAltText()));
        config.setFaviconUrl(trim(command.faviconUrl()));
        config.setFontFamilyHref(trim(command.fontFamilyHref()));
        config.setUpdatedBy(actor == null ? "unknown" : actor);
        config.setUpdatedAt(Instant.now());

        BrandingConfig saved = repository.save(config);
        LOG.info(
                "Branding fuer Mandant {} aktualisiert auf Version {} durch {}",
                tenantId,
                saved.getVersion(),
                actor);
        return toView(saved);
    }

    @Transactional
    public BrandingView rollbackForCurrentTenant(int targetVersion, String actor) {
        UUID tenantId = resolveTenantId();
        BrandingConfigHistory snapshot = historyRepository
                .findByTenantIdAndVersion(tenantId, targetVersion)
                .orElseThrow(() -> new UnknownBrandingVersionException(
                        "Keine Branding-History fuer Version " + targetVersion
                                + " des Mandanten " + tenantId + " gefunden."));
        int currentVersion = repository
                .findByTenantId(tenantId)
                .map(BrandingConfig::getVersion)
                .orElse(0);
        BrandingUpdateCommand rollbackCommand = new BrandingUpdateCommand(
                snapshot.getPrimaryColor(),
                snapshot.getPrimaryContrastColor(),
                snapshot.getAccentColor(),
                snapshot.getFontFamilyName(),
                snapshot.getFontFamilyMonoName(),
                snapshot.getAppTitle(),
                snapshot.getLogoUrl(),
                snapshot.getLogoAltText(),
                snapshot.getFaviconUrl(),
                snapshot.getFontFamilyHref(),
                currentVersion);
        LOG.info(
                "Rollback der Branding-Konfiguration fuer Mandant {} auf Version {} durch {}",
                tenantId,
                targetVersion,
                actor);
        return updateForCurrentTenant(rollbackCommand, actor);
    }

    @Transactional(readOnly = true)
    public List<BrandingHistoryEntry> history(int limit) {
        UUID tenantId = resolveTenantId();
        int sicheresLimit = limit <= 0 ? 20 : Math.min(limit, 200);
        return historyRepository
                .findByTenantIdOrderByVersionDesc(
                        tenantId, PageRequest.of(0, sicheresLimit))
                .stream()
                .map(BrandingService::toHistoryEntry)
                .toList();
    }

    private UUID resolveTenantId() {
        return TenantContext.current()
                .orElseGet(() -> tenantLookup
                        .findDefaultTenantId()
                        .orElseThrow(() -> new IllegalStateException(
                                "Kein Default-Mandant gefunden.")));
    }

    private static void validateColors(BrandingUpdateCommand command) {
        String primary = command.primaryColor();
        String contrast = command.primaryContrastColor();
        if (primary == null || primary.isBlank()) {
            throw new IllegalArgumentException("Primaerfarbe fehlt.");
        }
        if (contrast == null || contrast.isBlank()) {
            throw new IllegalArgumentException("Kontrastfarbe fehlt.");
        }
        if (!ContrastValidator.meetsAa(primary, contrast)) {
            throw new ContrastViolationException(
                    "Kontrast zwischen Primaer- und Kontrastfarbe "
                            + "unter WCAG AA (4.5:1).");
        }
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BrandingView toView(BrandingConfig config) {
        return new BrandingView(
                config.getPrimaryColor(),
                config.getPrimaryContrastColor(),
                config.getAccentColor(),
                config.getFontFamilyName(),
                config.getFontFamilyMonoName(),
                config.getAppTitle() != null
                        ? config.getAppTitle()
                        : BrandingDefaults.APP_TITLE,
                config.getLogoUrl(),
                config.getLogoAltText() != null
                        ? config.getLogoAltText()
                        : BrandingDefaults.LOGO_ALT,
                config.getFaviconUrl(),
                config.getFontFamilyHref(),
                config.getVersion() == null ? 1 : config.getVersion());
    }

    private static BrandingConfigHistory snapshotFromConfig(
            BrandingConfig config, String actor) {
        return BrandingConfigHistory.builder()
                .historyId(UUID.randomUUID())
                .tenantId(config.getTenantId())
                .primaryColor(config.getPrimaryColor())
                .primaryContrastColor(config.getPrimaryContrastColor())
                .accentColor(config.getAccentColor())
                .fontFamilyName(config.getFontFamilyName())
                .fontFamilyMonoName(config.getFontFamilyMonoName())
                .appTitle(config.getAppTitle())
                .logoUrl(config.getLogoUrl())
                .logoAltText(config.getLogoAltText())
                .faviconUrl(config.getFaviconUrl())
                .fontFamilyHref(config.getFontFamilyHref())
                .version(config.getVersion() == null ? 0 : config.getVersion())
                .updatedAt(config.getUpdatedAt() == null
                        ? Instant.now() : config.getUpdatedAt())
                .updatedBy(config.getUpdatedBy() == null
                        ? "unknown" : config.getUpdatedBy())
                .recordedAt(Instant.now())
                .recordedBy(actor == null ? "unknown" : actor)
                .build();
    }

    private static BrandingHistoryEntry toHistoryEntry(BrandingConfigHistory row) {
        return new BrandingHistoryEntry(
                row.getVersion() == null ? 0 : row.getVersion(),
                row.getPrimaryColor(),
                row.getPrimaryContrastColor(),
                row.getAccentColor(),
                row.getFontFamilyName(),
                row.getFontFamilyMonoName(),
                row.getAppTitle(),
                row.getLogoUrl(),
                row.getLogoAltText(),
                row.getFaviconUrl(),
                row.getFontFamilyHref(),
                row.getUpdatedAt(),
                row.getUpdatedBy(),
                row.getRecordedAt(),
                row.getRecordedBy());
    }

    /** Wird geworfen, wenn der Kontrast unter 4.5:1 liegt. */
    public static final class ContrastViolationException extends RuntimeException {
        public ContrastViolationException(String message) {
            super(message);
        }
    }

    /** Wird geworfen, wenn ein Rollback auf eine nicht existierende Version zielt. */
    public static final class UnknownBrandingVersionException extends RuntimeException {
        public UnknownBrandingVersionException(String message) {
            super(message);
        }
    }
}
