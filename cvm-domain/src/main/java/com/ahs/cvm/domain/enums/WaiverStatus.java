package com.ahs.cvm.domain.enums;

/**
 * Lebenszyklus eines Waivers (Iteration 20, CVM-51).
 *
 * <ul>
 *   <li>{@link #ACTIVE} - Risiko-Akzeptanz gilt.</li>
 *   <li>{@link #EXPIRING_SOON} - 30 Tage vor {@code validUntil}.</li>
 *   <li>{@link #EXPIRED} - {@code validUntil} ueberschritten.
 *       Assessment wird auf NEEDS_REVIEW gehoben.</li>
 *   <li>{@link #REVOKED} - manuell widerrufen.</li>
 * </ul>
 */
public enum WaiverStatus {
    ACTIVE,
    EXPIRING_SOON,
    EXPIRED,
    REVOKED
}
