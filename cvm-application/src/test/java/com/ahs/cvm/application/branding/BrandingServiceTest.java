package com.ahs.cvm.application.branding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.application.branding.BrandingService.ContrastViolationException;
import com.ahs.cvm.application.branding.BrandingService.UnknownBrandingVersionException;
import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.application.tenant.TenantLookupService;
import com.ahs.cvm.persistence.branding.BrandingConfig;
import com.ahs.cvm.persistence.branding.BrandingConfigHistory;
import com.ahs.cvm.persistence.branding.BrandingConfigHistoryRepository;
import com.ahs.cvm.persistence.branding.BrandingConfigRepository;
import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class BrandingServiceTest {

    private static final UUID TENANT_ID = UUID.randomUUID();

    private BrandingConfigRepository repository;
    private BrandingConfigHistoryRepository historyRepository;
    private TenantLookupService tenantLookup;
    private BrandingService service;

    @BeforeEach
    void setUp() {
        repository = mock(BrandingConfigRepository.class);
        historyRepository = mock(BrandingConfigHistoryRepository.class);
        tenantLookup = mock(TenantLookupService.class);
        service = new BrandingService(repository, historyRepository, tenantLookup);
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
    @DisplayName("Update: schreibt vorherigen Stand vor Speichern in History")
    void updateHistorisiert() {
        BrandingConfig existing = config("#006ec7", "#ffffff", 4);
        given(repository.findByTenantId(TENANT_ID)).willReturn(Optional.of(existing));
        given(repository.save(any(BrandingConfig.class))).willAnswer(inv -> inv.getArgument(0));

        BrandingUpdateCommand command = command("#003a68", "#ffffff", 4);
        service.updateForCurrentTenant(command, "admin");

        verify(historyRepository)
                .save(any(BrandingConfigHistory.class));
    }

    @Test
    @DisplayName("Rollback: stellt vergangene Version ueber regulaeres Update wieder her")
    void rollbackStelltVersionWiederHer() {
        BrandingConfig current = config("#ff0000", "#ffffff", 7);
        BrandingConfigHistory historyEintrag = BrandingConfigHistory.builder()
                .historyId(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .primaryColor("#006ec7")
                .primaryContrastColor("#ffffff")
                .fontFamilyName("Fira Sans")
                .fontFamilyMonoName("Fira Code")
                .appTitle("CVE-Relevance-Manager")
                .version(3)
                .updatedAt(Instant.parse("2026-04-17T10:00:00Z"))
                .updatedBy("admin")
                .recordedAt(Instant.parse("2026-04-17T10:05:00Z"))
                .recordedBy("admin")
                .build();

        given(repository.findByTenantId(TENANT_ID)).willReturn(Optional.of(current));
        given(historyRepository.findByTenantIdAndVersion(TENANT_ID, 3))
                .willReturn(Optional.of(historyEintrag));
        given(repository.save(any(BrandingConfig.class))).willAnswer(inv -> inv.getArgument(0));

        BrandingView wiederhergestellt = service.rollbackForCurrentTenant(3, "admin");

        assertThat(wiederhergestellt.primaryColor()).isEqualTo("#006ec7");
        verify(historyRepository).save(any(BrandingConfigHistory.class));
    }

    @Test
    @DisplayName("Rollback: unbekannte Version wirft UnknownBrandingVersion")
    void rollbackUnbekannteVersion() {
        given(repository.findByTenantId(TENANT_ID))
                .willReturn(Optional.of(config("#006ec7", "#ffffff", 3)));
        given(historyRepository.findByTenantIdAndVersion(TENANT_ID, 99))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.rollbackForCurrentTenant(99, "admin"))
                .isInstanceOf(UnknownBrandingVersionException.class);
    }

    @Test
    @DisplayName("History: liefert begrenzte absteigende Liste pro Mandant")
    void historyListe() {
        BrandingConfigHistory alt = BrandingConfigHistory.builder()
                .historyId(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .primaryColor("#111111")
                .primaryContrastColor("#ffffff")
                .fontFamilyName("Fira Sans")
                .appTitle("alt")
                .version(1)
                .updatedAt(Instant.parse("2026-04-16T08:00:00Z"))
                .updatedBy("admin")
                .recordedAt(Instant.parse("2026-04-16T08:30:00Z"))
                .recordedBy("admin")
                .build();
        given(historyRepository.findByTenantIdOrderByVersionDesc(
                eq(TENANT_ID), any(Pageable.class)))
                .willReturn(List.of(alt));

        List<BrandingHistoryEntry> result = service.history(20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).version()).isEqualTo(1);
        assertThat(result.get(0).primaryColor()).isEqualTo("#111111");
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
