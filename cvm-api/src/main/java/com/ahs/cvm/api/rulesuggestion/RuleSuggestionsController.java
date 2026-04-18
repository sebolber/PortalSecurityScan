package com.ahs.cvm.api.rulesuggestion;

import com.ahs.cvm.ai.ruleextraction.ApproveRuleResult;
import com.ahs.cvm.ai.ruleextraction.RuleSuggestionService;
import com.ahs.cvm.ai.ruleextraction.RuleSuggestionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rules/suggestions")
@Tag(name = "RuleSuggestions", description = "KI-extrahierte Regel-Vorschlaege")
public class RuleSuggestionsController {

    private final RuleSuggestionService service;

    public RuleSuggestionsController(RuleSuggestionService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Offene Regel-Vorschlaege")
    public ResponseEntity<List<RuleSuggestionView>> liste() {
        return ResponseEntity.ok(service.listeOffene());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('CVM_ADMIN')")
    @Operation(summary = "Regel-Vorschlag freigeben (Vier-Augen).")
    public ResponseEntity<ApproveRuleResult> approve(
            @PathVariable("id") UUID suggestionId,
            @Valid @RequestBody ApproveRequest body) {
        return ResponseEntity.ok(service.approveAsView(suggestionId, body.approvedBy()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('CVM_ADMIN')")
    @Operation(summary = "Regel-Vorschlag ablehnen (Kommentar pflicht).")
    public ResponseEntity<RuleSuggestionView> reject(
            @PathVariable("id") UUID suggestionId,
            @Valid @RequestBody RejectRequest body) {
        return ResponseEntity.ok(
                service.rejectAsView(suggestionId, body.rejectedBy(), body.comment()));
    }

    public record ApproveRequest(@NotBlank String approvedBy) {}

    public record RejectRequest(@NotBlank String rejectedBy, @NotBlank String comment) {}
}
