package com.ahs.cvm.api.profileassistant;

import com.ahs.cvm.ai.profileassistant.ProfileAssistantService;
import com.ahs.cvm.ai.profileassistant.ProfileAssistantService.FinalizeResult;
import com.ahs.cvm.ai.profileassistant.ProfileAssistantService.ReplyResult;
import com.ahs.cvm.ai.profileassistant.ProfileAssistantService.StartResult;
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

@RestController
@RequestMapping("/api/v1/environments/{environmentId}/profile/assist")
@Tag(name = "ProfileAssistant", description = "Dialog-Editor fuer Kontextprofile")
public class ProfileAssistantController {

    private final ProfileAssistantService service;

    public ProfileAssistantController(ProfileAssistantService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Neue Assistant-Session starten.")
    public ResponseEntity<StartResult> start(
            @PathVariable UUID environmentId,
            @Valid @RequestBody StartRequest body) {
        return ResponseEntity.ok(service.start(environmentId, body.startedBy()));
    }

    @PostMapping("/{sessionId}/reply")
    @Operation(summary = "Antwort fuer die offene Frage.")
    public ResponseEntity<ReplyResult> reply(
            @PathVariable UUID environmentId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody ReplyRequest body) {
        return ResponseEntity.ok(service.reply(sessionId, body.fieldPath(), body.answer()));
    }

    @PostMapping("/{sessionId}/finalize")
    @Operation(summary = "Erzeugt einen Profil-Draft aus dem Dialog.")
    public ResponseEntity<FinalizeResult> finalize(
            @PathVariable UUID environmentId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody FinalizeRequest body) {
        return ResponseEntity.ok(service.finalize(sessionId, body.proposedBy()));
    }

    public record StartRequest(@NotBlank String startedBy) {}
    public record ReplyRequest(@NotBlank String fieldPath, @NotBlank String answer) {}
    public record FinalizeRequest(@NotBlank String proposedBy) {}
}
