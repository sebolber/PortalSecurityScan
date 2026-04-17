package com.ahs.cvm.domain.enums;

/**
 * Herkunft eines Assessment-Vorschlags. Bildet die Cascade
 * {@code REUSE → RULE → AI_SUGGESTION → HUMAN} ab.
 */
public enum ProposalSource {
    REUSE,
    RULE,
    AI_SUGGESTION,
    HUMAN
}
