package com.ahs.cvm.api.reachability;

import com.ahs.cvm.ai.reachability.ReachabilityAgent;
import com.ahs.cvm.ai.reachability.ReachabilityRequest;
import com.ahs.cvm.ai.reachability.ReachabilityResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Endpoint fuer die Reachability-Analyse (Iteration 15, CVM-40).
 */
@RestController
@RequestMapping("/api/v1/findings")
@Tag(name = "Reachability", description = "Reachability-Analyse pro Finding")
public class ReachabilityController {

    private final ReachabilityAgent agent;

    public ReachabilityController(ReachabilityAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/{id}/reachability")
    @Operation(summary = "Startet die Reachability-Analyse fuer ein Finding.")
    public ResponseEntity<ReachabilityResult> analyze(
            @PathVariable("id") UUID findingId,
            @Valid @RequestBody ReachabilityApiRequest body) {
        ReachabilityResult result = agent.analyze(new ReachabilityRequest(
                findingId,
                body.repoUrl(),
                body.branch(),
                body.commitSha(),
                body.vulnerableSymbol(),
                body.language(),
                body.instruction(),
                body.triggeredBy()));
        return ResponseEntity.ok(result);
    }

    public record ReachabilityApiRequest(
            @NotBlank String repoUrl,
            String branch,
            String commitSha,
            @NotBlank String vulnerableSymbol,
            String language,
            String instruction,
            @NotBlank String triggeredBy) {}
}
