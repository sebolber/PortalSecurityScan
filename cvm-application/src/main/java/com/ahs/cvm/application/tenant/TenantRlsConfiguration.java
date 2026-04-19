package com.ahs.cvm.application.tenant;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Iteration 62E (CVM-62): Transaktions-Manager bekommt einen expliziten
 * Order-Wert, damit der {@link TenantSessionAspect} mit einem niedrigeren
 * {@code @Order} AFTER ihm laufen kann (d.h. das SET LOCAL wird
 * innerhalb der laufenden Transaktion ausgefuehrt).
 *
 * <p>Spring-Boot aktiviert Transaktionen standardmaessig mit
 * {@code Ordered.LOWEST_PRECEDENCE}; damit haette ein Aspect per
 * Definition keine Chance, INSIDE der Tx zu laufen. Wir setzen den
 * Transaction-Order auf einen festen Wert, gegen den sich andere
 * Aspects sauber positionieren koennen.
 */
@Configuration
@EnableTransactionManagement(order = TenantRlsConfiguration.TX_ORDER)
public class TenantRlsConfiguration {

    /** Order-Wert des Transaction-Advice. */
    public static final int TX_ORDER = 100;

    /** Order-Wert des TenantSessionAspect - muss groesser als TX_ORDER sein. */
    public static final int TENANT_ASPECT_ORDER = 200;
}
