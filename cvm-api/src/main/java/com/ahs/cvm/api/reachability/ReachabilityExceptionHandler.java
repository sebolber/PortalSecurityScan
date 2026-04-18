package com.ahs.cvm.api.reachability;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = ReachabilityController.class)
public class ReachabilityExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        // Sonderfall: "Finding nicht gefunden" -> 404
        if (e.getMessage() != null && e.getMessage().contains("Finding nicht gefunden")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "finding_not_found", "message", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "reachability_bad_request", "message", e.getMessage()));
    }
}
