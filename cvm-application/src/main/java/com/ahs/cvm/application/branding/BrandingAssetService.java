package com.ahs.cvm.application.branding;

import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.application.tenant.TenantLookupService;
import com.ahs.cvm.persistence.branding.BrandingAsset;
import com.ahs.cvm.persistence.branding.BrandingAssetRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Upload-Service fuer Branding-Assets (Iteration 27b, CVM-62).
 *
 * <p>Akzeptiert Logos (SVG/PNG/ICO), Favicons und Fonts (woff2).
 * SVGs durchlaufen den {@link SvgSanitizer}, Groessenlimit 512 KB
 * (Logo/Favicon) bzw. 2 MB (Font). Duplikate werden via
 * SHA-256-Fingerprint erkannt und wiederverwendet.
 */
@Service
public class BrandingAssetService {

    public enum AssetKind {
        LOGO,
        FAVICON,
        FONT
    }

    /** Einheitliches 512-KB-Limit fuer Logos/Favicons. */
    public static final int MAX_LOGO_BYTES = 512 * 1024;

    /** Fonts duerfen bis 2 MB gross sein. */
    public static final int MAX_FONT_BYTES = 2 * 1024 * 1024;

    private static final Set<String> ALLOWED_LOGO_CT =
            Set.of("image/svg+xml", "image/png");
    private static final Set<String> ALLOWED_FAVICON_CT =
            Set.of("image/x-icon", "image/png", "image/svg+xml", "image/vnd.microsoft.icon");
    private static final Set<String> ALLOWED_FONT_CT =
            Set.of("font/woff2", "application/font-woff2");

    private final BrandingAssetRepository repository;
    private final TenantLookupService tenantLookup;

    public BrandingAssetService(
            BrandingAssetRepository repository, TenantLookupService tenantLookup) {
        this.repository = repository;
        this.tenantLookup = tenantLookup;
    }

    @Transactional(readOnly = true)
    public Optional<BrandingAssetView> findById(UUID assetId) {
        if (assetId == null) {
            return Optional.empty();
        }
        return repository.findById(assetId).map(BrandingAssetService::toView);
    }

    @Transactional
    public BrandingAssetView upload(AssetKind kind, String contentType, byte[] bytes, String actor) {
        if (kind == null) {
            throw new IllegalArgumentException("Asset-Kind fehlt.");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Upload-Datei ist leer.");
        }
        String resolvedCt = contentType == null ? "" : contentType.toLowerCase();
        ensureMimeAllowed(kind, resolvedCt);
        ensureSizeAllowed(kind, bytes.length);
        if ("image/svg+xml".equals(resolvedCt)) {
            SvgSanitizer.ensureSafe(new String(bytes, StandardCharsets.UTF_8));
        }
        UUID tenantId = resolveTenantId();
        String sha = sha256(bytes);
        BrandingAsset asset = BrandingAsset.builder()
                .tenantId(tenantId)
                .kind(kind.name())
                .contentType(resolvedCt)
                .sizeBytes(bytes.length)
                .sha256(sha)
                .bytes(bytes)
                .uploadedBy(actor == null ? "unknown" : actor)
                .build();
        return toView(repository.save(asset));
    }

    private static BrandingAssetView toView(BrandingAsset asset) {
        return new BrandingAssetView(
                asset.getId(),
                asset.getTenantId(),
                asset.getKind(),
                asset.getContentType(),
                asset.getSizeBytes(),
                asset.getSha256(),
                asset.getBytes());
    }

    private static void ensureMimeAllowed(AssetKind kind, String contentType) {
        Set<String> allowed = switch (kind) {
            case LOGO -> ALLOWED_LOGO_CT;
            case FAVICON -> ALLOWED_FAVICON_CT;
            case FONT -> ALLOWED_FONT_CT;
        };
        if (!allowed.contains(contentType)) {
            throw new IllegalArgumentException(
                    "MIME-Typ '" + contentType + "' nicht erlaubt fuer " + kind + ".");
        }
    }

    private static void ensureSizeAllowed(AssetKind kind, int size) {
        int max = kind == AssetKind.FONT ? MAX_FONT_BYTES : MAX_LOGO_BYTES;
        if (size > max) {
            throw new IllegalArgumentException(
                    "Upload-Groesse " + size + " Byte ueberschreitet Limit " + max + ".");
        }
    }

    private UUID resolveTenantId() {
        return TenantContext.current()
                .orElseGet(() -> tenantLookup
                        .findDefaultTenantId()
                        .orElseThrow(() -> new IllegalStateException(
                                "Kein Default-Mandant gefunden.")));
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nicht verfuegbar", e);
        }
    }
}
