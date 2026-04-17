package com.ahs.cvm.application.assessment;

import java.util.UUID;

public class AssessmentNotFoundException extends RuntimeException {

    public AssessmentNotFoundException(UUID id) {
        super("Assessment nicht gefunden: " + id);
    }

    public AssessmentNotFoundException(String message) {
        super(message);
    }
}
