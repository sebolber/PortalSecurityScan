package com.ahs.cvm.api.profile;

import com.ahs.cvm.application.profile.ContextProfileService;
import com.ahs.cvm.application.profile.ProfileFieldDiff;
import com.ahs.cvm.application.profile.ProfileNotFoundException;
import com.ahs.cvm.application.profile.ProfileView;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Endpunkte fuer Kontextprofile.
 *
 * <ul>
 *   <li>{@code GET /api/v1/environments/{id}/profile}</li>
 *   <li>{@code PUT /api/v1/environments/{id}/profile}</li>
 *   <li>{@code POST /api/v1/profiles/{versionId}/approve}</li>
 *   <li>{@code GET /api/v1/profiles/{versionId}/diff?against=latest}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Kontextprofil", description = "Versionierte Profile je Umgebung")
public class ProfileController {

    private final ContextProfileService profileService;

    public ProfileController(ContextProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/environments/{environmentId}/profile")
    @Operation(summary = "Aktuell aktive Profil-Version einer Umgebung abrufen")
    public ResponseEntity<ProfileResponse> aktuellesProfil(
            @PathVariable UUID environmentId) {
        return profileService
                .latestActiveFor(environmentId)
                .map(this::abbilden)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/environments/{environmentId}/profile")
    @PreAuthorize("hasAnyAuthority('CVM_PROFILE_AUTHOR','CVM_ADMIN')")
    @Operation(
            summary = "Neuen Profil-Draft fuer eine Umgebung anlegen",
            description = "Liefert die Draft-ID. Die Aktivierung erfolgt ueber /approve.")
    public ResponseEntity<ProfileResponse> draftAnlegen(
            @PathVariable UUID environmentId,
            @Valid @RequestBody ProfilePutRequest request) {
        ProfileView draft = profileService.proposeNewVersion(
                environmentId, request.yamlSource(), request.proposedBy());
        URI location = URI.create("/api/v1/profiles/" + draft.id());
        return ResponseEntity.created(location).body(abbilden(draft));
    }

    @PostMapping("/profiles/{profileVersionId}/approve")
    @PreAuthorize("hasAnyAuthority('CVM_PROFILE_APPROVER','CVM_ADMIN')")
    @Operation(summary = "Profil-Draft im Vier-Augen-Prinzip aktivieren")
    public ResponseEntity<ProfileResponse> freigeben(
            @PathVariable UUID profileVersionId,
            @Valid @RequestBody ProfileApproveRequest request) {
        ProfileView aktiv = profileService.approve(profileVersionId, request.approverId());
        return ResponseEntity.ok(abbilden(aktiv));
    }

    @GetMapping("/profiles/{profileVersionId}/diff")
    @Operation(summary = "Feldweisen Diff zu einer anderen Version liefern")
    public ResponseEntity<List<ProfileDiffEntry>> diff(
            @PathVariable UUID profileVersionId,
            @RequestParam(value = "against", defaultValue = "latest") String against) {
        UUID altId = aufloeseGegenparameter(profileVersionId, against);
        List<ProfileFieldDiff> diffs = profileService.diff(altId, profileVersionId);
        return ResponseEntity.ok(diffs.stream().map(ProfileDiffEntry::from).toList());
    }

    private UUID aufloeseGegenparameter(UUID version, String against) {
        if ("latest".equalsIgnoreCase(against)) {
            UUID envId = profileService
                    .environmentOf(version)
                    .orElseThrow(() -> new ProfileNotFoundException(version));
            return profileService
                    .latestActiveFor(envId)
                    .map(ProfileView::id)
                    .orElseThrow(() -> new ProfileNotFoundException(
                            "Keine aktive Vorgaengerversion gefunden."));
        }
        try {
            return UUID.fromString(against);
        } catch (IllegalArgumentException e) {
            throw new ProfileNotFoundException(
                    "Parameter 'against' muss 'latest' oder eine UUID sein: " + against);
        }
    }

    private ProfileResponse abbilden(ProfileView p) {
        return new ProfileResponse(
                p.id(),
                p.environmentId(),
                p.versionNumber(),
                p.state(),
                p.yamlSource(),
                p.proposedBy(),
                p.approvedBy(),
                p.approvedAt(),
                p.validFrom());
    }
}
