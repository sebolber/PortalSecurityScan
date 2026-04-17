package com.ahs.cvm.api.profile;

import com.ahs.cvm.application.profile.FourEyesViolationException;
import com.ahs.cvm.application.profile.ProfileNotFoundException;
import com.ahs.cvm.application.profile.ProfileValidationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = ProfileController.class)
public class ProfileExceptionHandler {

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ProfileNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "profile_not_found", "message", e.getMessage()));
    }

    @ExceptionHandler(ProfileValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ProfileValidationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "profile_validation_error", "message", e.getMessage()));
    }

    @ExceptionHandler(FourEyesViolationException.class)
    public ResponseEntity<Map<String, Object>> handleFourEyes(FourEyesViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "four_eyes_violation", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "profile_state_conflict", "message", e.getMessage()));
    }
}
