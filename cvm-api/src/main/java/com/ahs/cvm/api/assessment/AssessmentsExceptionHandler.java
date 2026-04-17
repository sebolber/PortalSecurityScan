package com.ahs.cvm.api.assessment;

import com.ahs.cvm.api.finding.FindingsController;
import com.ahs.cvm.application.assessment.AssessmentFourEyesViolationException;
import com.ahs.cvm.application.assessment.AssessmentNotFoundException;
import com.ahs.cvm.application.assessment.InvalidAssessmentTransitionException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Bildet die Assessment-spezifischen Fehler auf 400/404/409 mit
 * deutschsprachigen Fehlercodes ab.
 */
@ControllerAdvice(assignableTypes = {AssessmentsController.class, FindingsController.class})
public class AssessmentsExceptionHandler {

    @ExceptionHandler(AssessmentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(AssessmentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "assessment_not_found", "message", e.getMessage()));
    }

    @ExceptionHandler(AssessmentFourEyesViolationException.class)
    public ResponseEntity<Map<String, Object>> handleFourEyes(
            AssessmentFourEyesViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "assessment_four_eyes_violation",
                        "message", e.getMessage()));
    }

    @ExceptionHandler(InvalidAssessmentTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleTransition(
            InvalidAssessmentTransitionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "assessment_state_conflict",
                        "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "assessment_bad_request", "message", e.getMessage()));
    }
}
