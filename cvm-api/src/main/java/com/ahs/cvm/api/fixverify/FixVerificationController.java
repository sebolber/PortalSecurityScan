package com.ahs.cvm.api.fixverify;

import com.ahs.cvm.ai.fixverify.FixVerificationResult;
import com.ahs.cvm.ai.fixverify.FixVerificationService;
import com.ahs.cvm.ai.fixverify.FixVerificationService.FixVerificationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Endpunkt fuer die Fix-Verifikation (Iteration 16, CVM-41).
 */
@RestController
@RequestMapping("/api/v1/mitigations")
@Tag(name = "FixVerification", description = "LLM-Fix-Verifikation pro Mitigation-Plan")
public class FixVerificationController {

    private final FixVerificationService service;

    public FixVerificationController(FixVerificationService service) {
        this.service = service;
    }

    @PostMapping("/{id}/verify-fix")
    @Operation(summary = "Startet die Fix-Verifikation (synchron).")
    public ResponseEntity<FixVerificationResult> verify(
            @PathVariable("id") UUID mitigationId,
            @Valid @RequestBody VerifyFixApiRequest body) {
        FixVerificationResult result = service.verify(new FixVerificationRequest(
                mitigationId,
                body.repoUrl(),
                body.fromVersion(),
                body.toVersion(),
                body.vulnerableSymbol(),
                body.triggeredBy()));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/verification")
    @Operation(summary = "Liefert die aktuell im Plan gespeicherte Verifikation.")
    public ResponseEntity<FixVerificationResult> get(
            @PathVariable("id") UUID mitigationId) {
        return ResponseEntity.ok(service.load(mitigationId));
    }

    public record VerifyFixApiRequest(
            @NotBlank String repoUrl,
            @NotBlank String fromVersion,
            String toVersion,
            String vulnerableSymbol,
            @NotBlank String triggeredBy) {}
}
