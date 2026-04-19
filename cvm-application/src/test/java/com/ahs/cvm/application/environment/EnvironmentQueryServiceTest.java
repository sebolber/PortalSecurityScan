package com.ahs.cvm.application.environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.environment.EnvironmentQueryService.CreateEnvironmentCommand;
import com.ahs.cvm.application.environment.EnvironmentQueryService.EnvironmentKeyAlreadyExistsException;
import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.domain.enums.EnvironmentStage;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnvironmentQueryServiceTest {

    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    private EnvironmentRepository repo;
    private EnvironmentQueryService service;

    @BeforeEach
    void setUp() {
        repo = mock(EnvironmentRepository.class);
        given(repo.save(any(Environment.class))).willAnswer(inv -> {
            Environment e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID());
            }
            return e;
        });
        service = new EnvironmentQueryService(repo);
        TenantContext.set(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("create: gueltige Eingabe persistiert und liefert View")
    void createOk() {
        given(repo.findByTenantIdAndKey(TENANT, "CI")).willReturn(Optional.empty());

        EnvironmentView view = service.create(new CreateEnvironmentCommand(
                "CI", "Continuous Integration", EnvironmentStage.DEV, "default"));

        assertThat(view.key()).isEqualTo("CI");
        assertThat(view.stage()).isEqualTo(EnvironmentStage.DEV);
        assertThat(view.tenant()).isEqualTo("default");
        assertThat(view.id()).isNotNull();
    }

    @Test
    @DisplayName("create: doppelter key innerhalb des Mandanten -> Konflikt")
    void duplicate() {
        given(repo.findByTenantIdAndKey(TENANT, "CI"))
                .willReturn(Optional.of(Environment.builder().tenantId(TENANT).key("CI").build()));

        assertThatThrownBy(() -> service.create(new CreateEnvironmentCommand(
                        "CI", "CI", EnvironmentStage.DEV, null)))
                .isInstanceOf(EnvironmentKeyAlreadyExistsException.class);
    }

    @Test
    @DisplayName("create: leerer key fuehrt zu IllegalArgumentException")
    void leererKey() {
        assertThatThrownBy(() -> service.create(new CreateEnvironmentCommand(
                        "  ", "Name", EnvironmentStage.DEV, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key");
    }

    @Test
    @DisplayName("create: fehlende stage fuehrt zu IllegalArgumentException")
    void fehlendeStage() {
        assertThatThrownBy(() -> service.create(new CreateEnvironmentCommand(
                        "CI", "Name", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stage");
    }

    @Test
    @DisplayName("create: leerer tenant wird zu null")
    void leererTenant() {
        given(repo.findByTenantIdAndKey(TENANT, "CI")).willReturn(Optional.empty());
        EnvironmentView view = service.create(new CreateEnvironmentCommand(
                "CI", "CI", EnvironmentStage.DEV, "  "));
        assertThat(view.tenant()).isNull();
    }

    @Test
    @DisplayName("create: ohne Tenant-Kontext wirft IllegalStateException")
    void createOhneTenant() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.create(new CreateEnvironmentCommand(
                        "CI", "CI", EnvironmentStage.DEV, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("listAll liest nur aktive Umgebungen des aktuellen Mandanten")
    void list_nur_aktive() {
        Environment aktiv = Environment.builder().id(UUID.randomUUID())
                .tenantId(TENANT)
                .key("REF").name("REF").stage(EnvironmentStage.REF).build();
        given(repo.findByTenantIdAndDeletedAtIsNullOrderByKeyAsc(TENANT))
                .willReturn(List.of(aktiv));

        List<EnvironmentView> views = service.listAll();

        assertThat(views).hasSize(1);
        assertThat(views.get(0).key()).isEqualTo("REF");
    }

    @Test
    @DisplayName("listAll liefert leere Liste ohne Tenant-Kontext")
    void listOhneTenant() {
        TenantContext.clear();
        assertThat(service.listAll()).isEmpty();
    }

    @Test
    @DisplayName("loesche: setzt deletedAt auf jetzt und speichert")
    void loesche_setzt_deletedAt() {
        UUID id = UUID.randomUUID();
        Environment vorhanden = Environment.builder().id(id)
                .tenantId(TENANT)
                .key("REF").name("REF").stage(EnvironmentStage.REF).build();
        given(repo.findById(id)).willReturn(Optional.of(vorhanden));

        service.loesche(id);

        assertThat(vorhanden.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("loesche: unbekannte ID -> EntityNotFoundException")
    void loesche_unbekannte_id() {
        UUID id = UUID.randomUUID();
        given(repo.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.loesche(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("loesche: bereits geloeschte Umgebung bleibt unveraendert (Idempotenz)")
    void loesche_idempotent() {
        UUID id = UUID.randomUUID();
        java.time.Instant vorher = java.time.Instant.parse("2026-01-01T00:00:00Z");
        Environment vorhanden = Environment.builder().id(id)
                .tenantId(TENANT)
                .key("REF").name("REF").stage(EnvironmentStage.REF)
                .deletedAt(vorher).build();
        given(repo.findById(id)).willReturn(Optional.of(vorhanden));

        service.loesche(id);

        assertThat(vorhanden.getDeletedAt()).isEqualTo(vorher);
    }

    @Test
    @DisplayName("loesche: null-ID -> IllegalArgumentException")
    void loesche_null_id() {
        assertThatThrownBy(() -> service.loesche(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("loesche: Umgebung aus anderem Mandanten -> EntityNotFoundException")
    void loesche_fremder_mandant() {
        UUID id = UUID.randomUUID();
        Environment fremd = Environment.builder().id(id)
                .tenantId(UUID.randomUUID())
                .key("REF").name("REF").stage(EnvironmentStage.REF).build();
        given(repo.findById(id)).willReturn(Optional.of(fremd));

        assertThatThrownBy(() -> service.loesche(id))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
