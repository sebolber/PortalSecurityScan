package com.ahs.cvm.api.report;

import com.ahs.cvm.application.report.ReportNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Fehlerzuordnung fuer den {@link ReportsController}: 404 bei
 * unbekanntem Report, 400 bei Validierungsfehlern im Input.
 */
@ControllerAdvice(assignableTypes = ReportsController.class)
public class ReportsExceptionHandler {

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ReportNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "report_not_found",
                        "message", e.getMessage(),
                        "reportId", e.reportId().toString()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "report_bad_request", "message", e.getMessage()));
    }
}
