package com.ahs.cvm.api.llmconfig;

import com.ahs.cvm.application.llmconfig.LlmConfigurationCommands;
import com.ahs.cvm.application.llmconfig.LlmConfigurationService;
import com.ahs.cvm.application.llmconfig.LlmConfigurationView;
import com.ahs.cvm.application.llmconfig.ProviderDefaults;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD fuer {@code llm_configuration} (Iteration 34, CVM-78).
 *
 * <p>Alle Endpunkte erfordern {@code CVM_ADMIN}. Secrets werden
 * niemals im Klartext ausgeliefert; Responses enthalten nur
 * {@code secretSet} + {@code secretHint}.
 */
@RestController
@RequestMapping("/api/v1/admin/llm-configurations")
@Tag(name = "LLM-Konfigurationen", description = "Mandanten-Zugaenge zu LLM-Anbietern")
public class LlmConfigurationController {

    private final LlmConfigurationService service;

    public LlmConfigurationController(LlmConfigurationService service) {
        this.service = service;
    }

    @GetMapping("/providers")
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "Liste aller unterstuetzten Provider samt Default-Base-URL")
    public ResponseEntity<List<Map<String, Object>>> providers() {
        List<Map<String, Object>> list = ProviderDefaults.PROVIDERS.stream()
                .sorted()
                .map(p -> {
                    Map<String, Object> entry = new java.util.LinkedHashMap<>();
                    entry.put("provider", p);
                    entry.put("defaultBaseUrl", ProviderDefaults
                            .defaultBaseUrl(p).orElse(null));
                    entry.put("requiresExplicitBaseUrl",
                            ProviderDefaults.requiresExplicitBaseUrl(p));
                    return entry;
                })
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "Alle LLM-Konfigurationen des aktuellen Mandanten auflisten")
    public ResponseEntity<List<LlmConfigurationView>> list() {
        return ResponseEntity.ok(service.listForCurrentTenant());
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "Aktive LLM-Konfiguration des aktuellen Mandanten")
    public ResponseEntity<LlmConfigurationView> active() {
        Optional<LlmConfigurationView> active = service.activeForCurrentTenant();
        return active.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "Eine LLM-Konfiguration abrufen")
    public ResponseEntity<LlmConfigurationView> byId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "Neue LLM-Konfiguration anlegen")
    public ResponseEntity<LlmConfigurationView> create(
            @RequestBody LlmConfigurationRequests.Create req,
            Principal principal) {
        if (req == null) {
            throw new IllegalArgumentException("Request-Body fehlt.");
        }
        String actor = principal != null ? principal.getName() : "anonymous";
        LlmConfigurationView saved = service.create(
                new LlmConfigurationCommands.Create(
                        req.name(), req.description(), req.provider(),
                        req.model(), req.baseUrl(), req.secret(),
                        req.maxTokens(), req.temperature(),
                        Boolean.TRUE.equals(req.active())),
                actor);
        URI location = URI.create(
                "/api/v1/admin/llm-configurations/" + saved.id());
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "LLM-Konfiguration aktualisieren (Teil-Update)")
    public ResponseEntity<LlmConfigurationView> update(
            @PathVariable UUID id,
            @RequestBody LlmConfigurationRequests.Update req,
            Principal principal) {
        if (req == null) {
            throw new IllegalArgumentException("Request-Body fehlt.");
        }
        String actor = principal != null ? principal.getName() : "anonymous";
        LlmConfigurationView saved = service.update(id,
                new LlmConfigurationCommands.Update(
                        req.name(), req.description(), req.provider(),
                        req.model(), req.baseUrl(), req.secret(),
                        Boolean.TRUE.equals(req.secretClear()),
                        req.maxTokens(), req.temperature(), req.active()),
                actor);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CVM_ADMIN')")
    @Operation(summary = "LLM-Konfiguration loeschen")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
