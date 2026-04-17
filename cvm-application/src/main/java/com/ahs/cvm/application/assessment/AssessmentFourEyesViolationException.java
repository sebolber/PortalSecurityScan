package com.ahs.cvm.application.assessment;

/**
 * Vier-Augen-Verstoss im Assessment-Workflow.
 *
 * <p>Tritt beim Downgrade auf {@code NOT_APPLICABLE} oder
 * {@code INFORMATIONAL} auf, wenn Autor und Approver identisch sind.
 */
public class AssessmentFourEyesViolationException extends RuntimeException {

    public AssessmentFourEyesViolationException(String message) {
        super(message);
    }
}
