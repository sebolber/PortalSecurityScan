package com.ahs.cvm.api.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Extrahiert die Keycloak-Realm-Rollen aus dem JWT-Claim
 * {@code realm_access.roles} und legt sie als {@link GrantedAuthority}
 * ab (Iteration 23, CVM-54).
 *
 * <p>Fuer jede Rolle werden ZWEI Authorities gesetzt, damit sowohl
 * {@code @PreAuthorize("hasRole('CVM_ADMIN')")} (das Spring-intern
 * {@code ROLE_CVM_ADMIN} erwartet) als auch
 * {@code @PreAuthorize("hasAuthority('CVM_ADMIN')")} funktionieren.
 * Zusaetzlich liefert der Default-Converter die Scopes aus dem
 * {@code scope}-Claim.
 */
public final class KeycloakJwtAuthoritiesConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String CLAIM_REALM_ACCESS = "realm_access";
    private static final String CLAIM_RESOURCE_ACCESS = "resource_access";
    private static final String CLAIM_ROLES = "roles";

    private final JwtGrantedAuthoritiesConverter scopeConverter;

    public KeycloakJwtAuthoritiesConverter() {
        this.scopeConverter = new JwtGrantedAuthoritiesConverter();
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.addAll(scopeConverter.convert(jwt));
        for (String role : sammleRealmRollen(jwt)) {
            authorities.add(new SimpleGrantedAuthority(role));
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return List.copyOf(authorities);
    }

    static List<String> sammleRealmRollen(Jwt jwt) {
        List<String> out = new ArrayList<>();
        Object realmAccess = jwt.getClaim(CLAIM_REALM_ACCESS);
        if (realmAccess instanceof Map<?, ?> m) {
            out.addAll(rollenAus(m));
        }
        Object resourceAccess = jwt.getClaim(CLAIM_RESOURCE_ACCESS);
        if (resourceAccess instanceof Map<?, ?> m) {
            for (Object entry : m.values()) {
                if (entry instanceof Map<?, ?> client) {
                    out.addAll(rollenAus(client));
                }
            }
        }
        return out;
    }

    private static List<String> rollenAus(Map<?, ?> m) {
        Object roles = m.get(CLAIM_ROLES);
        if (!(roles instanceof Collection<?> c)) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (Object r : c) {
            if (r == null) {
                continue;
            }
            String name = r.toString().trim();
            if (!name.isBlank()) {
                out.add(name.toUpperCase(Locale.ROOT));
            }
        }
        return out;
    }
}
