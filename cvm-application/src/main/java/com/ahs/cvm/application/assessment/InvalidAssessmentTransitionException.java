package com.ahs.cvm.application.assessment;

import com.ahs.cvm.domain.enums.AssessmentStatus;

/**
 * Wird geworfen, wenn die {@link AssessmentStateMachine} einen nicht
 * erlaubten Status-Uebergang feststellt.
 */
public class InvalidAssessmentTransitionException extends RuntimeException {

    public InvalidAssessmentTransitionException(AssessmentStatus von, AssessmentStatus nach) {
        super("Ungueltiger Assessment-Uebergang: " + von + " -> " + nach);
    }
}
