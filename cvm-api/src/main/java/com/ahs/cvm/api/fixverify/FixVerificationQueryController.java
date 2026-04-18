package com.ahs.cvm.api.fixverify;

import com.ahs.cvm.application.fixverification.FixVerificationQueryService;
import com.ahs.cvm.application.fixverification.FixVerificationSummaryView;
import com.ahs.cvm.domain.enums.FixVerificationGrade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-Fassade fuer die Fix-Verifikations-Uebersicht
 * (Iteration 27e, CVM-65). Erganzt den bestehenden
 * {@code FixVerificationController}, der nur per-Mitigation-
 * Zugriff bietet.
 */
@RestController
@RequestMapping("/api/v1/fix-verification")
@Tag(name = "FixVerification", description = "Uebersicht: offene und verifizierte Fix-Verifikationen")
public class FixVerificationQueryController {

    private final FixVerificationQueryService service;

    public FixVerificationQueryController(FixVerificationQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Letzte Mitigation-Plaene mit ihrem Verifikations-Grade")
    public ResponseEntity<List<FixVerificationSummaryView>> list(
            @RequestParam(name = "grade", required = false) FixVerificationGrade grade,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        List<FixVerificationSummaryView> rows =
                grade == null ? service.recent(limit) : service.byGrade(grade, limit);
        return ResponseEntity.ok(rows);
    }
}
