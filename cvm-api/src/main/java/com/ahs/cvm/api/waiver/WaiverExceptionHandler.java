package com.ahs.cvm.api.waiver;

import com.ahs.cvm.application.waiver.WaiverNotApplicableException;
import com.ahs.cvm.application.waiver.WaiverService.VierAugenViolationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = WaiverController.class)
public class WaiverExceptionHandler {

    @ExceptionHandler(WaiverNotApplicableException.class)
    public ResponseEntity<Map<String, Object>> notApplicable(
            WaiverNotApplicableException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "waiver_not_applicable",
                        "message", e.getMessage()));
    }

    @ExceptionHandler(VierAugenViolationException.class)
    public ResponseEntity<Map<String, Object>> vierAugen(
            VierAugenViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "vier_augen_violation",
                        "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        if (e.getMessage() != null && e.getMessage().contains("nicht gefunden")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "waiver_not_found",
                            "message", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "waiver_bad_request",
                        "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "waiver_conflict",
                        "message", e.getMessage()));
    }
}
