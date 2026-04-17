package com.ahs.cvm.application.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ahs.cvm.domain.enums.AssessmentStatus;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AssessmentStateMachineTest {

    private static final Set<AssessmentStatus> TERMINAL = EnumSet.of(
            AssessmentStatus.REJECTED, AssessmentStatus.SUPERSEDED, AssessmentStatus.EXPIRED);

    private final AssessmentStateMachine stateMachine = new AssessmentStateMachine();

    @ParameterizedTest
    @EnumSource(
            value = AssessmentStatus.class,
            names = {"APPROVED", "REJECTED", "NEEDS_REVIEW", "SUPERSEDED"})
    @DisplayName("PROPOSED darf in APPROVED, REJECTED, NEEDS_REVIEW oder SUPERSEDED uebergehen")
    void proposedErlaubteUebergaenge(AssessmentStatus ziel) {
        stateMachine.pruefeUebergang(AssessmentStatus.PROPOSED, ziel);
    }

    @ParameterizedTest
    @EnumSource(
            value = AssessmentStatus.class,
            names = {"EXPIRED", "SUPERSEDED"})
    @DisplayName("APPROVED darf nur in EXPIRED oder SUPERSEDED uebergehen")
    void approvedErlaubteUebergaenge(AssessmentStatus ziel) {
        stateMachine.pruefeUebergang(AssessmentStatus.APPROVED, ziel);
    }

    @ParameterizedTest
    @EnumSource(
            value = AssessmentStatus.class,
            names = {"PROPOSED", "SUPERSEDED"})
    @DisplayName("NEEDS_REVIEW darf nur in PROPOSED oder SUPERSEDED uebergehen")
    void needsReviewErlaubteUebergaenge(AssessmentStatus ziel) {
        stateMachine.pruefeUebergang(AssessmentStatus.NEEDS_REVIEW, ziel);
    }

    @ParameterizedTest
    @EnumSource(value = AssessmentStatus.class)
    @DisplayName("Terminalzustaende verbieten jeden Folge-Uebergang")
    void terminalOhneFolge(AssessmentStatus ziel) {
        for (AssessmentStatus terminal : TERMINAL) {
            assertThatThrownBy(() -> stateMachine.pruefeUebergang(terminal, ziel))
                    .isInstanceOf(InvalidAssessmentTransitionException.class);
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = AssessmentStatus.class,
            names = {"PROPOSED", "REJECTED", "NEEDS_REVIEW"})
    @DisplayName("APPROVED verbietet Rueckspruenge auf PROPOSED/REJECTED/NEEDS_REVIEW")
    void approvedKeinRuecksprung(AssessmentStatus ziel) {
        assertThatThrownBy(() ->
                        stateMachine.pruefeUebergang(AssessmentStatus.APPROVED, ziel))
                .isInstanceOf(InvalidAssessmentTransitionException.class);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("isTerminal markiert REJECTED, SUPERSEDED und EXPIRED")
    void isTerminalErkennt() {
        assertThat(stateMachine.isTerminal(AssessmentStatus.REJECTED)).isTrue();
        assertThat(stateMachine.isTerminal(AssessmentStatus.SUPERSEDED)).isTrue();
        assertThat(stateMachine.isTerminal(AssessmentStatus.EXPIRED)).isTrue();
        assertThat(stateMachine.isTerminal(AssessmentStatus.PROPOSED)).isFalse();
        assertThat(stateMachine.isTerminal(AssessmentStatus.APPROVED)).isFalse();
        assertThat(stateMachine.isTerminal(AssessmentStatus.NEEDS_REVIEW)).isFalse();
    }
}
