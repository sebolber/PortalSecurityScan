package com.ahs.cvm.api.aiaudit;

import com.ahs.cvm.ai.audit.AiCallAuditQueryService;
import com.ahs.cvm.ai.audit.AiCallAuditView;
import com.ahs.cvm.domain.enums.AiCallStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lese-Endpoint fuer den AI-Audit-Trail (Iteration 11 Nachzug).
 * Zugriff nur fuer Rollen {@code AI_AUDITOR} und {@code CVM_ADMIN}.
 *
 * <p>System- und User-Prompts werden bewusst NICHT exponiert (PII-/
 * Secret-Risiko); nur Metadaten.
 */
@RestController
@RequestMapping("/api/v1/ai/audits")
@Tag(name = "AiAudit", description = "Lese-Sicht ai_call_audit")
public class AiAuditController {

    private final AiCallAuditQueryService service;

    public AiAuditController(AiCallAuditQueryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('AI_AUDITOR','CVM_ADMIN')")
    @Operation(summary = "Listet ai_call_audit-Eintraege paginiert (AI_AUDITOR/CVM_ADMIN).")
    public ResponseEntity<AiAuditPage> liste(
            @RequestParam(name = "status", required = false) AiCallStatus status,
            @RequestParam(name = "useCase", required = false) String useCase,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Page<AiCallAuditView> result = service.liste(status, useCase, page, size);
        return ResponseEntity.ok(new AiAuditPage(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()));
    }

    public record AiAuditPage(
            List<AiCallAuditView> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {}
}
