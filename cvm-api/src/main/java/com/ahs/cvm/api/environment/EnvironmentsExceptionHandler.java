package com.ahs.cvm.api.environment;

import com.ahs.cvm.application.environment.EnvironmentQueryService.EnvironmentKeyAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = EnvironmentsController.class)
public class EnvironmentsExceptionHandler {

    @ExceptionHandler(EnvironmentKeyAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
            EnvironmentKeyAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "environment_key_exists", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "environment_validation", "message", ex.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "environment_not_found", "message", ex.getMessage()));
    }
}
