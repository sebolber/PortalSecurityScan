package com.ahs.cvm.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakJwtAuthoritiesConverterTest {

    private final KeycloakJwtAuthoritiesConverter converter =
            new KeycloakJwtAuthoritiesConverter();

    @Test
    @DisplayName("setzt fuer jede Realm-Rolle Authority und ROLE_-Variante")
    void realmRollen() {
        Jwt jwt = token(Map.of(
                "realm_access", Map.of("roles", List.of("CVM_ADMIN", "CVM_VIEWER")),
                "sub", "anna"));

        Collection<GrantedAuthority> out = converter.convert(jwt);

        assertThat(namen(out)).contains(
                "CVM_ADMIN", "ROLE_CVM_ADMIN",
                "CVM_VIEWER", "ROLE_CVM_VIEWER");
    }

    @Test
    @DisplayName("traegt Resource-Client-Rollen ebenfalls als Authority")
    void resourceRollen() {
        Jwt jwt = token(Map.of(
                "resource_access", Map.of(
                        "cvm-local", Map.of("roles", List.of("CVM_REPORTER")),
                        "other", Map.of("roles", List.of("NOISE"))),
                "sub", "anna"));

        Collection<GrantedAuthority> out = converter.convert(jwt);

        assertThat(namen(out)).contains(
                "CVM_REPORTER", "ROLE_CVM_REPORTER",
                "NOISE", "ROLE_NOISE");
    }

    @Test
    @DisplayName("ignoriert leere oder fehlende Claims")
    void leer() {
        Jwt jwt = token(Map.of("sub", "anna"));
        Collection<GrantedAuthority> out = converter.convert(jwt);
        assertThat(namen(out)).doesNotContain("ROLE_CVM_ADMIN");
    }

    @Test
    @DisplayName("mapped Rollen case-insensitive nach UPPERCASE")
    void caseInsensitive() {
        Jwt jwt = token(Map.of(
                "realm_access", Map.of("roles", List.of("cvm_admin", "Cvm_Reviewer")),
                "sub", "anna"));
        Collection<GrantedAuthority> out = converter.convert(jwt);
        assertThat(namen(out)).contains(
                "CVM_ADMIN", "ROLE_CVM_ADMIN",
                "CVM_REVIEWER", "ROLE_CVM_REVIEWER");
    }

    private static List<String> namen(Collection<GrantedAuthority> auths) {
        return auths.stream().map(GrantedAuthority::getAuthority).toList();
    }

    private static Jwt token(Map<String, Object> claims) {
        return new Jwt("token",
                Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                claims);
    }
}
