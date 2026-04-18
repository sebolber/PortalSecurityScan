package com.ahs.cvm.domain.enums;

/**
 * Quality-Grade fuer eine Fix-Verifikation (Iteration 16, CVM-41).
 *
 * <ul>
 *   <li>{@link #A} - Release-Notes oder Commit nennen die CVE-ID
 *       explizit.</li>
 *   <li>{@link #B} - Commit-Message adressiert die vulnerable
 *       Funktion/Klasse eindeutig ohne CVE-ID.</li>
 *   <li>{@link #C} - keine eindeutige Evidenz.</li>
 *   <li>{@link #UNKNOWN} - Verifikation noch nicht durchgefuehrt oder
 *       Quellen nicht erreichbar.</li>
 * </ul>
 */
public enum FixVerificationGrade {
    A,
    B,
    C,
    UNKNOWN
}
