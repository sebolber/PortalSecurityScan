package com.ahs.cvm.api.modelprofile;

import com.ahs.cvm.application.modelprofile.ModelProfileService;
import com.ahs.cvm.application.modelprofile.ModelProfileService.ModelProfileChangeView;
import com.ahs.cvm.application.modelprofile.ModelProfileService.SwitchCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wechsel und Historie des LLM-Modell-Profils (Iteration 21, CVM-52).
 */
@RestController
@RequestMapping("/api/v1/environments/{environmentId}/model-profile")
@Tag(name = "ModelProfile", description = "LLM-Modell-Profil pro Umgebung")
public class ModelProfileController {

    private final ModelProfileService service;

    public ModelProfileController(ModelProfileService service) {
        this.service = service;
    }

    @PostMapping("/switch")
    @Operation(summary = "Profil wechseln (Vier-Augen, Audit im change-log).")
    public ResponseEntity<ModelProfileChangeView> switchProfile(
            @PathVariable("environmentId") UUID environmentId,
            @Valid @RequestBody SwitchRequest body) {
        ModelProfileChangeView v = service.switchProfile(new SwitchCommand(
                environmentId, body.newProfileId(), body.changedBy(),
                body.fourEyesConfirmer(), body.reason()));
        return ResponseEntity.status(201).body(v);
    }

    @GetMapping("/history")
    @Operation(summary = "Historie der Profil-Wechsel fuer diese Umgebung.")
    public ResponseEntity<List<ModelProfileChangeView>> history(
            @PathVariable("environmentId") UUID environmentId) {
        return ResponseEntity.ok(service.historieFuerEnvironment(environmentId));
    }

    public record SwitchRequest(
            @NotNull UUID newProfileId,
            @NotBlank String changedBy,
            @NotBlank String fourEyesConfirmer,
            String reason) {}
}
