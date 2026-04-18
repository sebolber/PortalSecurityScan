package com.ahs.cvm.api.rag;

import com.ahs.cvm.ai.rag.IndexingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-Endpoint zum Re-Index der RAG-Pipeline (Iteration 12, CVM-31).
 */
@RestController
@RequestMapping("/api/v1/admin/rag")
@Tag(name = "RAG-Admin", description = "Re-Index der RAG-Pipeline")
public class RagAdminController {

    private final IndexingService indexingService;

    public RagAdminController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasAuthority('CVM_ADMIN')")
    @Operation(summary = "Vollindex aller Assessments und Advisories (CVM_ADMIN)")
    public ResponseEntity<ReindexResponse> reindex() {
        int chunks = indexingService.indexAll();
        return ResponseEntity.ok(new ReindexResponse(chunks));
    }

    public record ReindexResponse(int chunks) {}
}
