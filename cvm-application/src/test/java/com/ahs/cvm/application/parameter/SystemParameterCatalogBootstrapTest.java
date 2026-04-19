package com.ahs.cvm.application.parameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.persistence.parameter.SystemParameter;
import com.ahs.cvm.persistence.parameter.SystemParameterRepository;
import com.ahs.cvm.persistence.tenant.Tenant;
import com.ahs.cvm.persistence.tenant.TenantRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemParameterCatalogBootstrapTest {

    private SystemParameterRepository parameterRepository;
    private TenantRepository tenantRepository;
    private SystemParameterCatalogBootstrap bootstrap;
    private List<SystemParameter> gespeichert;

    @BeforeEach
    void setUp() {
        parameterRepository = mock(SystemParameterRepository.class);
        tenantRepository = mock(TenantRepository.class);
        bootstrap = new SystemParameterCatalogBootstrap(parameterRepository, tenantRepository);
        gespeichert = new ArrayList<>();
        given(parameterRepository.save(any(SystemParameter.class))).willAnswer(inv -> {
            SystemParameter p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            gespeichert.add(p);
            return p;
        });
    }

    @Test
    @DisplayName("Leere Tabelle: fuer jeden aktiven Mandanten wird jeder Katalog-Schluessel angelegt")
    void seedet_alle_schluessel_bei_leerer_tabelle() {
        UUID tenantId = UUID.randomUUID();
        given(tenantRepository.findAll()).willReturn(List.of(aktiverTenant(tenantId)));
        given(parameterRepository.findByTenantIdOrderByCategoryAscLabelAsc(tenantId))
                .willReturn(List.of());

        int angelegt = bootstrap.seedAllTenants();

        assertThat(angelegt).isEqualTo(SystemParameterCatalog.entries().size());
        assertThat(gespeichert).allMatch(p -> p.getTenantId().equals(tenantId));
        assertThat(gespeichert).extracting(SystemParameter::getParamKey)
                .containsExactlyInAnyOrderElementsOf(
                        SystemParameterCatalog.entries().stream()
                                .map(SystemParameterCatalogEntry::paramKey)
                                .toList());
    }

    @Test
    @DisplayName("Bereits vorhandene Keys werden nicht doppelt angelegt und bestehende Werte nicht ueberschrieben")
    void ueberschreibt_bestehende_werte_nicht() {
        UUID tenantId = UUID.randomUUID();
        given(tenantRepository.findAll()).willReturn(List.of(aktiverTenant(tenantId)));
        SystemParameter bestehend = SystemParameter.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .paramKey("cvm.ai.reachability.enabled")
                .label("Reachability")
                .category(SystemParameterCatalog.CATEGORY_AI_REACHABILITY)
                .type(com.ahs.cvm.domain.enums.SystemParameterType.BOOLEAN)
                .value("true")
                .defaultValue("false")
                .build();
        given(parameterRepository.findByTenantIdOrderByCategoryAscLabelAsc(tenantId))
                .willReturn(List.of(bestehend));

        int angelegt = bootstrap.seedAllTenants();

        assertThat(angelegt).isEqualTo(SystemParameterCatalog.entries().size() - 1);
        assertThat(gespeichert).extracting(SystemParameter::getParamKey)
                .doesNotContain("cvm.ai.reachability.enabled");
    }

    @Test
    @DisplayName("Inaktive Mandanten werden uebersprungen")
    void ueberspringt_inaktive_mandanten() {
        UUID inaktiv = UUID.randomUUID();
        given(tenantRepository.findAll()).willReturn(List.of(inaktiverTenant(inaktiv)));

        int angelegt = bootstrap.seedAllTenants();

        assertThat(angelegt).isZero();
        verify(parameterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ohne Mandanten wird nichts geseedet")
    void kein_mandant_kein_seed() {
        given(tenantRepository.findAll()).willReturn(List.of());

        int angelegt = bootstrap.seedAllTenants();

        assertThat(angelegt).isZero();
        verify(parameterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sensitive Katalog-Eintraege werden ohne Wert angelegt (kein Secret wird geseedet)")
    void sensitive_ohne_wert() {
        UUID tenantId = UUID.randomUUID();
        given(tenantRepository.findAll()).willReturn(List.of(aktiverTenant(tenantId)));
        given(parameterRepository.findByTenantIdOrderByCategoryAscLabelAsc(tenantId))
                .willReturn(List.of());

        bootstrap.seedAllTenants();

        // Alle gespeicherten Secrets haben value == null
        List<SystemParameter> secrets = gespeichert.stream()
                .filter(SystemParameter::isSensitive)
                .toList();
        assertThat(secrets).isNotEmpty();
        assertThat(secrets).allMatch(p -> p.getValue() == null,
                "Sensitive Eintraege duerfen beim Seeden keinen Wert tragen");
    }

    @Test
    @DisplayName("Mehrere Mandanten: jeder erhaelt einen vollstaendigen Katalog")
    void seedet_mehrere_mandanten() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        given(tenantRepository.findAll()).willReturn(List.of(aktiverTenant(a), aktiverTenant(b)));
        given(parameterRepository.findByTenantIdOrderByCategoryAscLabelAsc(a)).willReturn(List.of());
        given(parameterRepository.findByTenantIdOrderByCategoryAscLabelAsc(b)).willReturn(List.of());

        int angelegt = bootstrap.seedAllTenants();

        int katalog = SystemParameterCatalog.entries().size();
        assertThat(angelegt).isEqualTo(katalog * 2);
        long fuerA = gespeichert.stream().filter(p -> p.getTenantId().equals(a)).count();
        long fuerB = gespeichert.stream().filter(p -> p.getTenantId().equals(b)).count();
        assertThat(fuerA).isEqualTo(katalog);
        assertThat(fuerB).isEqualTo(katalog);
    }

    private Tenant aktiverTenant(UUID id) {
        return Tenant.builder()
                .id(id)
                .tenantKey("t-" + id)
                .name("Tenant")
                .active(true)
                .build();
    }

    private Tenant inaktiverTenant(UUID id) {
        return Tenant.builder()
                .id(id)
                .tenantKey("t-" + id)
                .name("Tenant")
                .active(false)
                .build();
    }
}
