package com.ahs.cvm.api.branding;

import com.ahs.cvm.application.branding.BrandingService.ContrastViolationException;
import com.ahs.cvm.application.branding.SvgSanitizer.SvgRejectedException;
import jakarta.persistence.OptimisticLockException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = BrandingController.class)
public class BrandingExceptionHandler {

    @ExceptionHandler(ContrastViolationException.class)
    public ResponseEntity<Map<String, Object>> handleContrast(
            ContrastViolationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "contrast_violation", "message", ex.getMessage()));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(
            OptimisticLockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "branding_stale", "message", ex.getMessage()));
    }

    @ExceptionHandler(SvgRejectedException.class)
    public ResponseEntity<Map<String, Object>> handleSvg(SvgRejectedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "svg_rejected", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "branding_validation", "message", ex.getMessage()));
    }
}
