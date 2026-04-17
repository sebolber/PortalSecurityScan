package com.ahs.cvm.api.rules;

import com.ahs.cvm.application.rules.RuleConditionException;
import com.ahs.cvm.application.rules.RuleFourEyesViolationException;
import com.ahs.cvm.application.rules.RuleNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = RulesController.class)
public class RulesExceptionHandler {

    @ExceptionHandler(RuleNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RuleNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "rule_not_found", "message", e.getMessage()));
    }

    @ExceptionHandler(RuleConditionException.class)
    public ResponseEntity<Map<String, Object>> handleCondition(RuleConditionException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "rule_condition_invalid", "message", e.getMessage()));
    }

    @ExceptionHandler(RuleFourEyesViolationException.class)
    public ResponseEntity<Map<String, Object>> handleFourEyes(RuleFourEyesViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "four_eyes_violation", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "rule_state_conflict", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "rule_bad_request", "message", e.getMessage()));
    }
}
