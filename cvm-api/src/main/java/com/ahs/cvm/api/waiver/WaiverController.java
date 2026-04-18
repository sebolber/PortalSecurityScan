package com.ahs.cvm.api.waiver;

import com.ahs.cvm.application.waiver.WaiverService;
import com.ahs.cvm.application.waiver.WaiverService.GrantCommand;
import com.ahs.cvm.application.waiver.WaiverView;
import com.ahs.cvm.domain.enums.WaiverStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Fassade fuer das Waiver-Management (Iteration 20, CVM-51).
 */
@RestController
@RequestMapping("/api/v1/waivers")
@Tag(name = "Waiver", description = "Risiko-Waiver (Vier-Augen-gesichert)")
public class WaiverController {

    private final WaiverService service;

    public WaiverController(WaiverService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Waiver anlegen (Vier-Augen, grantedBy != decidedBy).")
    public ResponseEntity<WaiverView> grant(@Valid @RequestBody GrantRequest body) {
        WaiverView view = service.grant(new GrantCommand(
                body.assessmentId(), body.reason(), body.grantedBy(),
                body.validUntil(), body.reviewIntervalDays()));
        return ResponseEntity.status(201).body(view);
    }

    @PostMapping("/{id}/extend")
    @Operation(summary = "Gueltigkeit verlaengern (Vier-Augen, extendedBy != grantedBy).")
    public ResponseEntity<WaiverView> extend(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ExtendRequest body) {
        return ResponseEntity.ok(service.extend(id, body.validUntil(), body.extendedBy()));
    }

    @PostMapping("/{id}/revoke")
    @Operation(summary = "Waiver widerrufen.")
    public ResponseEntity<WaiverView> revoke(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RevokeRequest body) {
        return ResponseEntity.ok(service.revoke(id, body.revokedBy(), body.reason()));
    }

    @GetMapping
    @Operation(summary = "Waiver nach Status auflisten.")
    public ResponseEntity<List<WaiverView>> list(
            @RequestParam(name = "status", defaultValue = "ACTIVE") WaiverStatus status) {
        return ResponseEntity.ok(service.byStatus(status));
    }

    public record GrantRequest(
            @NotNull UUID assessmentId,
            @NotBlank String reason,
            @NotBlank String grantedBy,
            @NotNull Instant validUntil,
            Integer reviewIntervalDays) {}

    public record ExtendRequest(
            @NotNull Instant validUntil,
            @NotBlank String extendedBy) {}

    public record RevokeRequest(
            @NotBlank String revokedBy,
            @NotBlank String reason) {}
}
