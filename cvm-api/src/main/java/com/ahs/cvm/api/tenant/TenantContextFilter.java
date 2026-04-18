package com.ahs.cvm.api.tenant;

import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.application.tenant.TenantLookupService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Liest den aktuellen Mandanten aus dem JWT und legt ihn via
 * {@link TenantContext} fuer die laufende Anfrage ab.
 *
 * <p>Claim-Prioritaet:
 * <ol>
 *   <li>{@code tenant_key} (String) - gepflegt in Keycloak-Mapper,</li>
 *   <li>{@code tid} (String) - Fallback-Kurzform,</li>
 *   <li>Default-Tenant aus der Datenbank.</li>
 * </ol>
 *
 * <p>Wird im JWT ein unbekannter oder inaktiver Key geliefert, greift
 * ebenfalls der Default-Tenant. Es ist bewusst tolerant, damit ein
 * Mandant-Rollout nicht den gesamten Request-Fluss bricht - die
 * RLS-Durchsetzung bleibt in einer eigenen Migration.
 *
 * <p>Der Context wird im {@code finally}-Block immer zurueckgesetzt,
 * damit der ThreadLocal nicht ueber den Request hinaus lebt
 * (Thread-Pool-Sicherheit).
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    static final String CLAIM_TENANT_KEY = "tenant_key";
    static final String CLAIM_FALLBACK_TID = "tid";

    private final TenantLookupService tenantLookup;

    public TenantContextFilter(TenantLookupService tenantLookup) {
        this.tenantLookup = tenantLookup;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            resolveTenant().ifPresent(TenantContext::set);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private Optional<UUID> resolveTenant() {
        String key = readClaimFromAuthentication();
        if (key != null) {
            Optional<UUID> byKey = tenantLookup.findIdByKey(key);
            if (byKey.isPresent()) {
                return byKey;
            }
            log.debug("Tenant-Key '{}' aus JWT unbekannt, falle auf Default zurueck.", key);
        }
        return tenantLookup.findDefaultTenantId();
    }

    private String readClaimFromAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return null;
        }
        Jwt jwt = jwtAuth.getToken();
        String primary = jwt.getClaimAsString(CLAIM_TENANT_KEY);
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        String fallback = jwt.getClaimAsString(CLAIM_FALLBACK_TID);
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }
}
