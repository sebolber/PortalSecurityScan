package com.ahs.cvm.domain.enums;

/** Art der Evidenz fuer die Fix-Verifikation (Iteration 16). */
public enum FixEvidenceType {
    EXPLICIT_CVE_MENTION,
    FIX_COMMIT_MATCH,
    INFERRED,
    NONE
}
