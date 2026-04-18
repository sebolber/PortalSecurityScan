package com.ahs.cvm.api.modelprofile;

import com.ahs.cvm.application.modelprofile.ModelProfileQueryService;
import com.ahs.cvm.application.modelprofile.ModelProfileService;
import com.ahs.cvm.application.modelprofile.ModelProfileService.CreateCommand;
import com.ahs.cvm.application.modelprofile.ModelProfileView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verwaltung der LLM-Modellprofile. Read-Liste und Admin-Anlage
 * verlangen beide {@code CVM_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/llm-model-profiles")
@Tag(name = "LlmModelProfiles", description = "LLM-Modellprofile (Read + Admin-Create)")
public class LlmModelProfilesController {

    private final ModelProfileQueryService queryService;
    private final ModelProfileService profileService;

    public LlmModelProfilesController(
            ModelProfileQueryService queryService,
            ModelProfileService profileService) {
        this.queryService = queryService;
        this.profileService = profileService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CVM_ADMIN')")
    @Operation(summary = "Alle Modellprofile auflisten (CVM_ADMIN).")
    public ResponseEntity<List<ModelProfileView>> list() {
        return ResponseEntity.ok(queryService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CVM_ADMIN')")
    @Operation(summary = "Neues LLM-Modellprofil anlegen (CVM_ADMIN).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Profil angelegt"),
        @ApiResponse(responseCode = "400", description = "Eingabe ungueltig"),
        @ApiResponse(responseCode = "409",
                description = "profileKey bereits vergeben oder Vier-Augen-Verstoss")
    })
    public ResponseEntity<ModelProfileView> anlegen(
            @Valid @RequestBody ModelProfileCreateRequest request) {
        ModelProfileView created = profileService.createProfile(new CreateCommand(
                request.profileKey(),
                request.provider(),
                request.modelId(),
                request.modelVersion(),
                request.costBudgetEurMonthly(),
                request.approvedForGkvData(),
                request.approvedBy(),
                request.fourEyesConfirmer(),
                request.reason()));
        return ResponseEntity
                .created(URI.create("/api/v1/llm-model-profiles/" + created.id()))
                .body(created);
    }
}
