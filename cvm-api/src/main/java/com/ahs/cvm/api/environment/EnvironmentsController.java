package com.ahs.cvm.api.environment;

import com.ahs.cvm.application.environment.EnvironmentQueryService;
import com.ahs.cvm.application.environment.EnvironmentView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-Endpunkt fuer die Umgebungen (Iteration 25, CVM-56).
 *
 * <p>Liefert die Basis-Liste fuer die Einstellungen-UI (Modell-
 * Profil-Wechsel) und die Profile-UI (aktive Profil-Version pro
 * Umgebung). Schreiboperationen sind nicht Teil dieses Controllers.
 */
@RestController
@RequestMapping("/api/v1/environments")
@Tag(name = "Environments", description = "Umgebungs-Liste (Read)")
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
}
