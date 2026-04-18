package com.ahs.cvm.api.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.application.tenant.TenantLookupService;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TenantContextFilterTest {

    private static final UUID TENANT_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DEFAULT_TENANT_ID =
            UUID.fromString("dddddddd-dddd-4ddd-dddd-dddddddddddd");

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("setzt Tenant aus dem JWT-Claim 'tenant_key'")
    void tenant_key_claim_greift() throws Exception {
        TenantLookupService lookup = mock(TenantLookupService.class);
        when(lookup.findIdByKey("bkk-nord")).thenReturn(Optional.of(TENANT_ID));

        TenantContextFilter filter = new TenantContextFilter(lookup);
        authenticateMitClaims(Map.of("tenant_key", "bkk-nord"));

        UUID[] beobachtet = new UUID[1];
        FilterChain chain = captureTenant(beobachtet);

        filter.doFilter(new MockHttpServletRequest(),
                new MockHttpServletResponse(), chain);

        assertThat(beobachtet[0]).isEqualTo(TENANT_ID);
        assertThat(TenantContext.current()).isEmpty();
    }

    @Test
    @DisplayName("faellt bei fehlendem Claim auf den Default-Tenant zurueck")
    void faellt_auf_default_zurueck() throws Exception {
        TenantLookupService lookup = mock(TenantLookupService.class);
        when(lookup.findDefaultTenantId()).thenReturn(Optional.of(DEFAULT_TENANT_ID));

        TenantContextFilter filter = new TenantContextFilter(lookup);
        authenticateMitClaims(Map.of("sub", "t.tester@ahs.test"));

        UUID[] beobachtet = new UUID[1];
        FilterChain chain = captureTenant(beobachtet);

        filter.doFilter(new MockHttpServletRequest(),
                new MockHttpServletResponse(), chain);

        assertThat(beobachtet[0]).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(TenantContext.current()).isEmpty();
    }

    @Test
    @DisplayName("unbekannter Claim-Key laesst Default greifen")
    void unbekannter_key_faellt_auf_default() throws Exception {
        TenantLookupService lookup = mock(TenantLookupService.class);
        when(lookup.findIdByKey("bkk-unbekannt")).thenReturn(Optional.empty());
        when(lookup.findDefaultTenantId()).thenReturn(Optional.of(DEFAULT_TENANT_ID));

        TenantContextFilter filter = new TenantContextFilter(lookup);
        authenticateMitClaims(Map.of("tenant_key", "bkk-unbekannt"));

        UUID[] beobachtet = new UUID[1];
        FilterChain chain = captureTenant(beobachtet);

        filter.doFilter(new MockHttpServletRequest(),
                new MockHttpServletResponse(), chain);

        assertThat(beobachtet[0]).isEqualTo(DEFAULT_TENANT_ID);
    }

    @Test
    @DisplayName("raeumt TenantContext auch bei Exception im Chain auf")
    void exception_raeumt_kontext_auf() {
        TenantLookupService lookup = mock(TenantLookupService.class);
        when(lookup.findDefaultTenantId()).thenReturn(Optional.of(DEFAULT_TENANT_ID));

        TenantContextFilter filter = new TenantContextFilter(lookup);
        authenticateMitClaims(Map.of());

        FilterChain chain = (req, res) -> {
            throw new RuntimeException("boom");
        };

        try {
            filter.doFilter(new MockHttpServletRequest(),
                    new MockHttpServletResponse(), chain);
        } catch (Exception ignored) {
            // erwartet
        }
        assertThat(TenantContext.current()).isEmpty();
    }

    @Test
    @DisplayName("ohne Authentifizierung greift ebenfalls der Default-Tenant")
    void ohne_auth_greift_default() throws Exception {
        TenantLookupService lookup = mock(TenantLookupService.class);
        when(lookup.findDefaultTenantId()).thenReturn(Optional.of(DEFAULT_TENANT_ID));

        TenantContextFilter filter = new TenantContextFilter(lookup);
        // kein SecurityContext gesetzt
        SecurityContextHolder.clearContext();

        UUID[] beobachtet = new UUID[1];
        FilterChain chain = captureTenant(beobachtet);

        filter.doFilter(new MockHttpServletRequest(),
                new MockHttpServletResponse(), chain);

        assertThat(beobachtet[0]).isEqualTo(DEFAULT_TENANT_ID);
    }

    private static FilterChain captureTenant(UUID[] target) {
        return (req, res) -> target[0] = TenantContext.current().orElse(null);
    }

    private static void authenticateMitClaims(Map<String, Object> claims) {
        Jwt jwt = new Jwt(
                "token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                claims.isEmpty() ? Map.of("sub", "t.tester@ahs.test") : claims);
        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
