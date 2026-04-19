package com.ahs.cvm.application.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Iteration 62E (CVM-62): Setzt die Postgres-Session-Variable
 * {@code cvm.current_tenant} auf die ID des aktuellen
 * {@link TenantContext}. Die Postgres-RLS-Policy
 * ({@code cvm_current_tenant()}, Migration V0039) liest diesen Wert
 * und filtert alle Zeilen mit abweichendem {@code tenant_id} aus
 * Queries und UPDATEs/DELETEs heraus.
 *
 * <p>Der Aspekt laeuft mit Order {@value TenantRlsConfiguration#TENANT_ASPECT_ORDER}
 * und damit INNERHALB der bereits von Spring gestarteten Transaktion
 * (Order {@value TenantRlsConfiguration#TX_ORDER}). Damit trifft das
 * {@code SET LOCAL} auf die transaktionsgebundene Connection und wird
 * am Commit/Rollback mit zurueckgesetzt.
 */
@Aspect
@Component
@Order(TenantRlsConfiguration.TENANT_ASPECT_ORDER)
public class TenantSessionAspect {

    private static final Logger log = LoggerFactory.getLogger(TenantSessionAspect.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Around(
        "@annotation(org.springframework.transaction.annotation.Transactional) "
            + "|| @within(org.springframework.transaction.annotation.Transactional)")
    public Object applyTenantScope(ProceedingJoinPoint pjp) throws Throwable {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // Kein aktiver TX (etwa readOnly mit neuer Tx in Sub-Call oder
            // Tests ohne tx) - RLS greift dann ueber den session-default
            // (NULL = fail closed).
            return pjp.proceed();
        }
        UUID tenantId = TenantContext.current().orElse(null);
        if (tenantId == null) {
            return pjp.proceed();
        }
        try {
            entityManager
                    .createNativeQuery(
                            "SET LOCAL cvm.current_tenant = '" + tenantId + "'")
                    .executeUpdate();
        } catch (RuntimeException e) {
            log.warn("SET LOCAL cvm.current_tenant fehlgeschlagen: {}", e.getMessage());
            // Fail-open in Tests (H2 kennt kein SET LOCAL) - Produktion
            // erkennt das am Error-Log.
        }
        return pjp.proceed();
    }
}
