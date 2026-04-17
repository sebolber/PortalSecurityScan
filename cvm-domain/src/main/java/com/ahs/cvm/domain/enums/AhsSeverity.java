package com.ahs.cvm.domain.enums;

/**
 * adesso-health-solutions-Severity fuer Assessments und Findings. Gilt fuer
 * das gesamte CVM unabhaengig vom CVSS-Score der Original-CVE.
 */
public enum AhsSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFORMATIONAL,
    NOT_APPLICABLE
}
