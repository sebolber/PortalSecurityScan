package com.ahs.cvm.api.environment;

import com.ahs.cvm.application.environment.EnvironmentQueryService;
import com.ahs.cvm.application.environment.EnvironmentQueryService.CreateEnvironmentCommand;
import com.ahs.cvm.application.environment.EnvironmentView;
import com.ahs.cvm.domain.enums.EnvironmentStage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * Read- und Anlage-Endpunkt fuer die Umgebungen (Iteration 25 + 28e).
 *
 * <p>Read (GET) steht allen eingeloggten Usern offen; die Anlage
 * (POST) erfordert die Rolle {@code CVM_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/environments")
@Tag(name = "Environments", description = "Umgebungs-Liste + Anlage")
public class EnvironmentsController {

    private final EnvironmentQueryService service;

    public EnvironmentsController(EnvironmentQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Alle Umgebungen auflisten (alphabetisch nach key).")
    public ResponseEntity<List<EnvironmentView>> list() {
        return ResponseEntity.ok(service.listAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CVM_ADMIN')")
    @Operation(summary = "Neue Umgebung anlegen (CVM_ADMIN).")
    public ResponseEntity<EnvironmentView> create(
            @Valid @RequestBody CreateEnvironmentRequest request) {
        EnvironmentView saved = service.create(new CreateEnvironmentCommand(
                request.key(), request.name(), request.stage(), request.tenant()));
        return ResponseEntity
                .created(URI.create("/api/v1/environments/" + saved.id()))
                .body(saved);
    }

    public record CreateEnvironmentRequest(
            @NotBlank String key,
            @NotBlank String name,
            @NotNull EnvironmentStage stage,
            String tenant) {}
}
