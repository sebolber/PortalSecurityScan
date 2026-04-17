package com.ahs.cvm.api.scan;

import com.ahs.cvm.application.scan.SbomParseException;
import com.ahs.cvm.application.scan.SbomSchemaException;
import com.ahs.cvm.application.scan.ScanAlreadyIngestedException;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = ScanController.class)
public class ScanExceptionHandler {

    @ExceptionHandler(SbomParseException.class)
    public ResponseEntity<Map<String, Object>> parseFehler(SbomParseException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "sbom_parse_error", "message", e.getMessage()));
    }

    @ExceptionHandler(SbomSchemaException.class)
    public ResponseEntity<Map<String, Object>> schemaFehler(SbomSchemaException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "sbom_schema_error", "message", e.getMessage()));
    }

    @ExceptionHandler(ScanAlreadyIngestedException.class)
    public ResponseEntity<Map<String, Object>> duplikat(ScanAlreadyIngestedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "scan_already_ingested",
                        "message", e.getMessage(),
                        "existingScanId", e.getExistingScanId().toString()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumentFehler(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "invalid_argument", "message", e.getMessage()));
    }

    @ExceptionHandler(RejectedExecutionException.class)
    public ResponseEntity<Map<String, Object>> ueberlastung(RejectedExecutionException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "ingest_queue_full", "message",
                        "SBOM-Queue ist voll. Bitte spaeter erneut hochladen."));
    }
}
