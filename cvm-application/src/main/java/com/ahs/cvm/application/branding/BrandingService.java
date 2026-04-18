package com.ahs.cvm.application.branding;

import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.application.tenant.TenantLookupService;
import com.ahs.cvm.persistence.branding.BrandingConfig;
import com.ahs.cvm.persistence.branding.BrandingConfigRepository;
import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lese-/Schreib-Service fuer Mandanten-Branding (Iteration 27, CVM-61).
 *
 * <p>{@link #loadForCurrentTenant()} liefert das Branding des aktuellen
 * (via {@link TenantContext}) oder - wenn keiner gesetzt - des
 * Default-Mandanten. Das Ergebnis enthaelt immer valide Werte: fehlt
 * ein Datensatz, greift {@link BrandingDefaults} mit der adesso-CI-
 * Vorbelegung.
 *
 * <p>{@link #updateForCurrentTenant(BrandingUpdateCommand, String)}
 * erzwingt WCAG-AA-Kontrast und optimistisches Locking. Assets
 * (Logo, Favicon, Font) werden in dieser Iteration ueber externe URLs
 * gepflegt; der Multipart-Upload folgt in 27b.
 */
@Service
public class BrandingService {

    private static final Logger LOG = LoggerFactory.getLogger(BrandingService.class);

    private final BrandingConfigRepository repository;
    private final TenantLookupService tenantLookup;

    public BrandingService(
            BrandingConfigRepository repository,
            TenantLookupService tenantLookup) {
        this.repository = repository;
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

    /** Wird geworfen, wenn der Kontrast unter 4.5:1 liegt. */
    public static final class ContrastViolationException extends RuntimeException {
        public ContrastViolationException(String message) {
            super(message);
        }
    }
}
