package com.ahs.cvm.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sichert die in {@code CLAUDE.md} und im Fachkonzept zugesagte Enum-Reihenfolge
 * ab. Viele Cascade- und Priorisierungs-Regeln stuetzen sich auf
 * {@link Enum#ordinal()}.
 */
class EnumTest {

    @Test
    @DisplayName("AhsSeverity: Reihenfolge CRITICAL>HIGH>...>NOT_APPLICABLE")
    void ahsSeverityReihenfolge() {
        assertThat(AhsSeverity.values())
                .containsExactly(
                        AhsSeverity.CRITICAL,
                        AhsSeverity.HIGH,
                        AhsSeverity.MEDIUM,
                        AhsSeverity.LOW,
                        AhsSeverity.INFORMATIONAL,
                        AhsSeverity.NOT_APPLICABLE);
    }

    @Test
    @DisplayName("ProposalSource: Cascade REUSE-RULE-AI-HUMAN in exakt dieser Reihenfolge")
    void proposalSourceCascade() {
        assertThat(ProposalSource.values())
                .containsExactly(
                        ProposalSource.REUSE,
                        ProposalSource.RULE,
                        ProposalSource.AI_SUGGESTION,
                        ProposalSource.HUMAN);
    }

    @Test
    @DisplayName("AssessmentStatus: PROPOSED ist der Eingangsstatus")
    void assessmentStatusEingang() {
        assertThat(AssessmentStatus.values()[0]).isEqualTo(AssessmentStatus.PROPOSED);
    }

    @Test
    @DisplayName("EnvironmentStage: PROD ist die hoechste Stufe")
    void environmentStageReihenfolge() {
        assertThat(EnvironmentStage.values())
                .containsExactly(
                        EnvironmentStage.DEV,
                        EnvironmentStage.TEST,
                        EnvironmentStage.REF,
                        EnvironmentStage.ABN,
                        EnvironmentStage.PROD);
    }
}
