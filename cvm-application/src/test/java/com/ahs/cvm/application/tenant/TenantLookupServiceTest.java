package com.ahs.cvm.application.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.persistence.tenant.Tenant;
import com.ahs.cvm.persistence.tenant.TenantRepository;
import java.time.Instant;
import java.util.List;
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
}
