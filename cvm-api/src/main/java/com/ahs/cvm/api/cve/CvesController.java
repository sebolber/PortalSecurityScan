package com.ahs.cvm.api.cve;

import com.ahs.cvm.application.cve.CveQueryService;
import com.ahs.cvm.application.cve.CveView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-Endpunkt fuer das CVE-Inventar (Iteration 26, CVM-57).
 */
@RestController
@RequestMapping("/api/v1/cves")
@Tag(name = "CVEs", description = "CVE-Inventar (Read)")
public class CvesController {

    private final CveQueryService service;

    public CvesController(CveQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "CVEs paginiert lesen, optional gefiltert nach Schlagwort/Severity/KEV.")
    public ResponseEntity<CvePageResponse> list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "severity", required = false) AhsSeverity severity,
            @RequestParam(value = "kev", defaultValue = "false") boolean kev,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Page<CveView> result = service.findPage(q, severity, kev, page, size);
        return ResponseEntity.ok(new CvePageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()));
    }

    public record CvePageResponse(
            List<CveView> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {}
}
