package com.ahs.cvm.api.modelprofile;

import com.ahs.cvm.application.modelprofile.ModelProfileQueryService;
import com.ahs.cvm.application.modelprofile.ModelProfileView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-Endpunkt fuer alle LLM-Modellprofile (Iteration 25, CVM-56).
 *
 * <p>Dient dem Einstellungen-Admin-Bereich als Dropdown-Quelle.
 * Nur fuer {@code CVM_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/llm-model-profiles")
@Tag(name = "LlmModelProfiles", description = "Alle verfuegbaren LLM-Modellprofile (Read)")
public class LlmModelProfilesController {

    private final ModelProfileQueryService service;

    public LlmModelProfilesController(ModelProfileQueryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CVM_ADMIN')")
    @Operation(summary = "Alle Modellprofile auflisten (CVM_ADMIN).")
    public ResponseEntity<List<ModelProfileView>> list() {
        return ResponseEntity.ok(service.listAll());
    }
}
