package com.ahs.cvm.api.anomaly;

import com.ahs.cvm.ai.anomaly.AnomalyQueryService;
import com.ahs.cvm.ai.anomaly.AnomalyQueryService.AnomalyView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Endpunkte fuer die Anomalie-Sicht (Iteration 18, CVM-43).
 * Liefert nur Lese-Sichten - keine Entscheidungshoheit.
 */
@RestController
@RequestMapping("/api/v1/anomalies")
@Tag(name = "Anomalies", description = "KI-Anomalie-Hinweise")
public class AnomalyController {

    private final AnomalyQueryService service;

    public AnomalyController(AnomalyQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Anomalie-Hinweise der letzten N Stunden (Default 24).")
    public ResponseEntity<List<AnomalyView>> liste(
            @RequestParam(name = "hours", defaultValue = "24") int hours) {
        Instant since = Instant.now().minus(Duration.ofHours(Math.max(1, hours)));
        return ResponseEntity.ok(service.liste(since));
    }

    @GetMapping("/count")
    @Operation(summary = "Anzahl Anomalien in den letzten 24 Stunden.")
    public ResponseEntity<AnomalyCount> count() {
        Instant since = Instant.now().minus(Duration.ofHours(24));
        return ResponseEntity.ok(new AnomalyCount(service.count24h(since)));
    }

    public record AnomalyCount(long count) {}
}
