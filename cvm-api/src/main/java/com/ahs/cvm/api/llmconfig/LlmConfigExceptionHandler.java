package com.ahs.cvm.api.llmconfig;

import com.ahs.cvm.application.llmconfig.LlmConfigurationService.LlmConfigurationNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = LlmConfigurationController.class)
public class LlmConfigExceptionHandler {

    @ExceptionHandler(LlmConfigurationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            LlmConfigurationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "llm_configuration_not_found",
                        "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(
            IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "llm_configuration_validation",
                        "message", ex.getMessage()));
    }
}
