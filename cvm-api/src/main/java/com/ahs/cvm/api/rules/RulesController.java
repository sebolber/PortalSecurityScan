package com.ahs.cvm.api.rules;

import com.ahs.cvm.application.rules.DryRunResult;
import com.ahs.cvm.application.rules.DryRunService;
import com.ahs.cvm.application.rules.RuleNotFoundException;
import com.ahs.cvm.application.rules.RuleService;
import com.ahs.cvm.application.rules.RuleService.RuleDraftInput;
import com.ahs.cvm.application.rules.RuleView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rules")
@Tag(name = "Regeln", description = "Deterministische Regel-Engine")
public class RulesController {

    private final RuleService ruleService;
    private final DryRunService dryRunService;

    public RulesController(RuleService ruleService, DryRunService dryRunService) {
        this.ruleService = ruleService;
        this.dryRunService = dryRunService;
    }

    @GetMapping
    @Operation(summary = "Alle Regeln auflisten")
    public ResponseEntity<List<RuleResponse>> list() {
        return ResponseEntity.ok(
                ruleService.listAll().stream().map(RuleResponse::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "Neue Regel als DRAFT anlegen")
    public ResponseEntity<RuleResponse> anlegen(@Valid @RequestBody RuleCreateRequest req) {
        RuleView draft = ruleService.proposeRule(
                new RuleDraftInput(
                        req.ruleKey(),
                        req.name(),
                        req.description(),
                        req.proposedSeverity(),
                        req.conditionJson(),
                        req.rationaleTemplate(),
                        req.rationaleSourceFields(),
                        req.origin()),
                req.createdBy());
        return ResponseEntity
                .created(URI.create("/api/v1/rules/" + draft.id()))
                .body(RuleResponse.from(draft));
    }

    @PostMapping("/{ruleId}/activate")
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "Regel im Vier-Augen-Prinzip aktivieren")
    public ResponseEntity<RuleResponse> aktivieren(
            @PathVariable UUID ruleId, @Valid @RequestBody RuleActivateRequest req) {
        RuleView aktiv = ruleService.activate(ruleId, req.approverId());
        return ResponseEntity.ok(RuleResponse.from(aktiv));
    }

    @PostMapping("/{ruleId}/dry-run")
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "Regel gegen historische Findings simulieren")
    public ResponseEntity<DryRunResponse> dryRun(
            @PathVariable UUID ruleId,
            @RequestParam(value = "days", defaultValue = "180") int days) {
        if (!ruleService.find(ruleId).isPresent()) {
            throw new RuleNotFoundException(ruleId);
        }
        DryRunResult result = dryRunService.dryRun(ruleId, days);
        return ResponseEntity.ok(DryRunResponse.from(result));
    }
}
