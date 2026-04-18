package com.ahs.cvm.application.branding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.application.branding.BrandingService.ContrastViolationException;
import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.application.tenant.TenantLookupService;
import com.ahs.cvm.persistence.branding.BrandingConfig;
import com.ahs.cvm.persistence.branding.BrandingConfigRepository;
import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BrandingServiceTest {

    private static final UUID TENANT_ID = UUID.randomUUID();

    private BrandingConfigRepository repository;
    private TenantLookupService tenantLookup;
    private BrandingService service;

    @BeforeEach
    void setUp() {
        repository = mock(BrandingConfigRepository.class);
        tenantLookup = mock(TenantLookupService.class);
        service = new BrandingService(repository, tenantLookup);
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Load: fehlende Zeile liefert adesso-Default-Branding")
    void defaultBranding() {
        given(repository.findByTenantId(TENANT_ID)).willReturn(Optional.empty());

        BrandingView view = service.loadForCurrentTenant();

        assertThat(view.primaryColor()).isEqualTo("#006ec7");
        assertThat(view.fontFamilyName()).isEqualTo("Fira Sans");
        assertThat(view.appTitle()).isEqualTo("CVE-Relevance-Manager");
    }

    @Test
    @DisplayName("Load: vorhandene Zeile wird 1:1 in View abgebildet")
    void vorhandeneZeile() {
        given(repository.findByTenantId(TENANT_ID))
                .willReturn(Optional.of(config("#123456", "#ffffff", 3)));

        BrandingView view = service.loadForCurrentTenant();

        assertThat(view.primaryColor()).isEqualTo("#123456");
        assertThat(view.version()).isEqualTo(3);
    }

    @Test
    @DisplayName("Update: Primaerfarbe mit zu geringem Kontrast wird abgelehnt")
    void kontrastVerletzt() {
        given(repository.findByTenantId(TENANT_ID))
                .willReturn(Optional.of(config("#006ec7", "#ffffff", 1)));

        BrandingUpdateCommand badCommand = command("#cccccc", "#ffffff", 1);

        assertThatThrownBy(() -> service.updateForCurrentTenant(badCommand, "admin"))
                .isInstanceOf(ContrastViolationException.class);
    }

    @Test
    @DisplayName("Update: veraltete Version loest Optimistic-Lock-Fehler aus")
    void veralteteVersion() {
        given(repository.findByTenantId(TENANT_ID))
                .willReturn(Optional.of(config("#006ec7", "#ffffff", 5)));

        BrandingUpdateCommand command = command("#006ec7", "#ffffff", 1);

        assertThatThrownBy(() -> service.updateForCurrentTenant(command, "admin"))
                .isInstanceOf(OptimisticLockException.class);
    }

    @Test
    @DisplayName("Update: gueltige Werte werden gespeichert")
    void gueltigesUpdate() {
        BrandingConfig existing = config("#006ec7", "#ffffff", 1);
        given(repository.findByTenantId(TENANT_ID)).willReturn(Optional.of(existing));
        given(repository.save(any(BrandingConfig.class))).willAnswer(inv -> inv.getArgument(0));

        BrandingUpdateCommand command = command("#003a68", "#ffffff", 1);
        BrandingView view = service.updateForCurrentTenant(command, "admin");

        assertThat(view.primaryColor()).isEqualTo("#003a68");
        verify(repository).save(any(BrandingConfig.class));
    }

    @Test
    @DisplayName("Update: ohne Tenant-Kontext faellt auf Default-Mandant zurueck")
    void fallbackDefault() {
        TenantContext.clear();
        UUID defaultTenant = UUID.randomUUID();
        given(tenantLookup.findDefaultTenantId()).willReturn(Optional.of(defaultTenant));
        given(repository.findByTenantId(defaultTenant)).willReturn(Optional.empty());

        BrandingView view = service.loadForCurrentTenant();
        assertThat(view.fontFamilyName()).isEqualTo("Fira Sans");
    }

    private static BrandingConfig config(String primary, String contrast, int version) {
        return BrandingConfig.builder()
                .tenantId(TENANT_ID)
                .primaryColor(primary)
                .primaryContrastColor(contrast)
                .fontFamilyName("Fira Sans")
                .fontFamilyMonoName("Fira Code")
                .appTitle("CVE-Relevance-Manager")
                .version(version)
                .updatedAt(Instant.now())
                .updatedBy("test")
                .build();
    }

    private static BrandingUpdateCommand command(String primary, String contrast, int version) {
        return new BrandingUpdateCommand(
                primary, contrast, "#887d75",
                "Fira Sans", "Fira Code",
                "CVE-Relevance-Manager",
                null, "adesso", null, null,
                version);
    }
}
