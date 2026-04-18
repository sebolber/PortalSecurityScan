package com.ahs.cvm.api.reachability;

import com.ahs.cvm.application.reachability.ReachabilityQueryService;
import com.ahs.cvm.application.reachability.ReachabilitySummaryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Uebersicht ueber die zuletzt erzeugten Reachability-Vorschlaege
 * (Iteration 27e, CVM-65). Ergaenzt den bestehenden
 * {@link ReachabilityController}, der nur per-Finding POSTs bietet.
 */
@RestController
@RequestMapping("/api/v1/reachability")
@Tag(name = "Reachability", description = "Reachability-Uebersicht ueber Mandant")
public class ReachabilityQueryController {

    private final ReachabilityQueryService service;

    public ReachabilityQueryController(ReachabilityQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Letzte Reachability-Analysen absteigend nach Zeit")
    public ResponseEntity<List<ReachabilitySummaryView>> list(
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(service.recent(limit));
    }
}
