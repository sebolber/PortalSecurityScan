package com.ahs.cvm.api.pipeline;

import com.ahs.cvm.application.pipeline.PipelineGateService.GateRateLimitException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = PipelineGateController.class)
public class PipelineGateExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "gate_bad_request",
                        "message", e.getMessage()));
    }

    @ExceptionHandler(GateRateLimitException.class)
    public ResponseEntity<Map<String, Object>> rateLimit(GateRateLimitException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "gate_rate_limited",
                        "message", e.getMessage()));
    }
}
