package com.ahs.cvm.application.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.tenant.TenantLookupService.TenantKeyAlreadyExistsException;
import com.ahs.cvm.persistence.tenant.Tenant;
import com.ahs.cvm.persistence.tenant.TenantRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantLookupServiceTest {

    private final TenantRepository repo = mock(TenantRepository.class);
    private final TenantLookupService service = new TenantLookupService(repo);

    @Test
    @DisplayName("listAll: Default-Tenant steht oben, Rest alphabetisch nach Key")
    void listAllSortierung() {
        Tenant a = Tenant.builder().id(UUID.randomUUID())
                .tenantKey("alpha").name("Alpha")
                .active(true).defaultTenant(false)
                .createdAt(Instant.now()).build();
        Tenant def = Tenant.builder().id(UUID.randomUUID())
                .tenantKey("main").name("Main")
                .active(true).defaultTenant(true)
                .createdAt(Instant.now()).build();
        Tenant b = Tenant.builder().id(UUID.randomUUID())
                .tenantKey("beta").name("Beta")
                .active(true).defaultTenant(false)
                .createdAt(Instant.now()).build();
        given(repo.findAll()).willReturn(List.of(a, def, b));

        List<TenantView> views = service.listAll();

        assertThat(views).extracting(TenantView::tenantKey)
                .containsExactly("main", "alpha", "beta");
        assertThat(views.get(0).defaultTenant()).isTrue();
    }

    @Test
    @DisplayName("listAll: leere Liste liefert leere View-Liste")
    void listAllLeer() {
        given(repo.findAll()).willReturn(List.of());
        assertThat(service.listAll()).isEmpty();
    }

    @Test
    @DisplayName("create: persistiert neuen Mandanten mit trimmed key/name")
    void createHappyPath() {
        given(repo.findByTenantKey("cvm-test")).willReturn(Optional.empty());
        given(repo.save(any(Tenant.class))).willAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(UUID.randomUUID());
            }
            if (t.getCreatedAt() == null) {
                t.setCreatedAt(Instant.now());
            }
            return t;
        });

        TenantView view = service.create("  cvm-test  ", " Testmandant ", true);

        assertThat(view.tenantKey()).isEqualTo("cvm-test");
        assertThat(view.name()).isEqualTo("Testmandant");
        assertThat(view.active()).isTrue();
        assertThat(view.defaultTenant()).isFalse();
    }

    @Test
    @DisplayName("create: doppelter key -> TenantKeyAlreadyExistsException")
    void createDoppelterKey() {
        given(repo.findByTenantKey("cvm-test"))
                .willReturn(Optional.of(Tenant.builder().tenantKey("cvm-test").build()));

        assertThatThrownBy(() -> service.create("cvm-test", "x", true))
                .isInstanceOf(TenantKeyAlreadyExistsException.class);
    }

    @Test
    @DisplayName("create: leerer key/name -> IllegalArgumentException")
    void createLeereFelder() {
        assertThatThrownBy(() -> service.create("  ", "name", true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create("key", "  ", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("setActive: Toggle eines regulaeren Mandanten ok")
    void setActiveRegulaer() {
        UUID id = UUID.randomUUID();
        Tenant t = Tenant.builder().id(id).tenantKey("x").name("X")
                .active(true).defaultTenant(false).createdAt(Instant.now()).build();
        given(repo.findById(id)).willReturn(Optional.of(t));
        given(repo.save(any(Tenant.class))).willAnswer(inv -> inv.getArgument(0));

        TenantView view = service.setActive(id, false);

        assertThat(view.active()).isFalse();
        assertThat(t.isActive()).isFalse();
    }

    @Test
    @DisplayName("setActive: Default-Mandant kann nicht deaktiviert werden")
    void setActiveDefaultNichtDeaktivierbar() {
        UUID id = UUID.randomUUID();
        Tenant t = Tenant.builder().id(id).tenantKey("main").name("Main")
                .active(true).defaultTenant(true).createdAt(Instant.now()).build();
        given(repo.findById(id)).willReturn(Optional.of(t));

        assertThatThrownBy(() -> service.setActive(id, false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("setActive: unbekannte Id -> EntityNotFoundException")
    void setActiveUnbekannt() {
        UUID id = UUID.randomUUID();
        given(repo.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.setActive(id, true))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }
}
