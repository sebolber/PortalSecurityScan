package com.ahs.cvm.application.branding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.branding.BrandingAssetService.AssetKind;
import com.ahs.cvm.application.branding.SvgSanitizer.SvgRejectedException;
import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.application.tenant.TenantLookupService;
import com.ahs.cvm.persistence.branding.BrandingAsset;
import com.ahs.cvm.persistence.branding.BrandingAssetRepository;
// Entity-Import wird weiterhin fuer Mock-Responses benoetigt, wenn
// der Test spaeter den Rueckgabewert des Repositories prueft.
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BrandingAssetServiceTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final byte[] SAUBERES_SVG =
            ("<svg xmlns=\"http://www.w3.org/2000/svg\"><rect width=\"10\" height=\"10\"/></svg>")
                    .getBytes(StandardCharsets.UTF_8);

    private BrandingAssetRepository repo;
    private TenantLookupService lookup;
    private BrandingAssetService service;

    @BeforeEach
    void setUp() {
        repo = mock(BrandingAssetRepository.class);
        lookup = mock(TenantLookupService.class);
        given(repo.save(any(BrandingAsset.class))).willAnswer(inv -> inv.getArgument(0));
        service = new BrandingAssetService(repo, lookup);
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Logo: sauberes SVG wird akzeptiert und mit SHA-256 gespeichert")
    void logoAkzeptiert() {
        BrandingAssetView saved = service.upload(
                AssetKind.LOGO, "image/svg+xml", SAUBERES_SVG, "admin");

        assertThat(saved.tenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.kind()).isEqualTo("LOGO");
        assertThat(saved.sha256()).hasSize(64);
        assertThat(saved.sizeBytes()).isEqualTo(SAUBERES_SVG.length);
    }

    @Test
    @DisplayName("Logo: SVG mit eingebettetem Script wird abgelehnt")
    void logoScriptAbgelehnt() {
        byte[] bad =
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>1</script></svg>"
                        .getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(
                        () -> service.upload(AssetKind.LOGO, "image/svg+xml", bad, "admin"))
                .isInstanceOf(SvgRejectedException.class);
    }

    @Test
    @DisplayName("Logo: unerlaubter MIME-Typ wird abgelehnt")
    void mimeAbgelehnt() {
        assertThatThrownBy(
                        () -> service.upload(
                                AssetKind.LOGO, "application/pdf", new byte[] {1, 2, 3}, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MIME");
    }

    @Test
    @DisplayName("Logo: Groessenlimit 512 KB wird erzwungen")
    void groesseAbgelehnt() {
        byte[] too_big = new byte[BrandingAssetService.MAX_LOGO_BYTES + 1];

        assertThatThrownBy(
                        () -> service.upload(AssetKind.LOGO, "image/png", too_big, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Groesse");
    }

    @Test
    @DisplayName("Font: nur woff2 wird akzeptiert")
    void fontMime() {
        byte[] woff2 = new byte[] {0x77, 0x4f, 0x46, 0x32};
        BrandingAssetView saved =
                service.upload(AssetKind.FONT, "font/woff2", woff2, "admin");
        assertThat(saved.kind()).isEqualTo("FONT");
    }

    @Test
    @DisplayName("Upload: leere Datei wird abgelehnt")
    void leer() {
        assertThatThrownBy(
                        () -> service.upload(AssetKind.LOGO, "image/svg+xml", new byte[0], "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
