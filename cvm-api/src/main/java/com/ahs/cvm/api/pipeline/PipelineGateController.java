package com.ahs.cvm.api.pipeline;

import com.ahs.cvm.application.pipeline.PipelineGateService;
import com.ahs.cvm.application.pipeline.PipelineGateService.GateRequest;
import com.ahs.cvm.application.pipeline.PipelineGateService.GateResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * CI/CD-Gate-Endpunkt (Iteration 21, CVM-52).
 */
@RestController
@RequestMapping("/api/v1/pipeline")
@Tag(name = "PipelineGate", description = "CI/CD-Gate (CycloneDX-SBOM -> PASS/WARN/FAIL)")
public class PipelineGateController {

    private final PipelineGateService service;

    public PipelineGateController(PipelineGateService service) {
        this.service = service;
    }

    @PostMapping(value = "/gate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pipeline-Gate: SBOM pruefen, PASS/WARN/FAIL liefern.")
    public ResponseEntity<GateResult> gate(
            @RequestParam("productVersionId") UUID productVersionId,
            @RequestParam(name = "environmentId", required = false) UUID environmentId,
            @RequestParam(name = "branchRef", required = false) String branchRef,
            @RequestParam(name = "mergeRequestId", required = false) String mergeRequestId,
            @RequestParam(name = "repoUrl", required = false) String repoUrl,
            @RequestParam("sbom") MultipartFile sbom) throws IOException {
        if (sbom == null || sbom.isEmpty()) {
            throw new IllegalArgumentException("SBOM-Datei fehlt oder ist leer.");
        }
        GateResult r = service.evaluate(new GateRequest(
                productVersionId, environmentId, branchRef, mergeRequestId,
                sbom.getBytes(), repoUrl));
        return ResponseEntity.ok()
                .header("X-Gate-Decision", r.gate().name())
                .body(r);
    }
}
