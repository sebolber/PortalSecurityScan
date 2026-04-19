package com.ahs.cvm.api.assessment;

import com.ahs.cvm.application.assessment.AssessmentWriteService;
import com.ahs.cvm.application.assessment.FindingQueueView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Endpunkte fuer Bewertungs-Workflow.
 *
 * <ul>
 *   <li>{@code POST /api/v1/assessments} &mdash; manueller Vorschlag.</li>
 *   <li>{@code POST /api/v1/assessments/{id}/approve} &mdash; Freigabe
 *       (Vier-Augen greift bei Downgrade auf NOT_APPLICABLE /
 *       INFORMATIONAL).</li>
 *   <li>{@code POST /api/v1/assessments/{id}/reject} &mdash; Ablehnung mit
 *       Pflicht-Kommentar.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/assessments")
@Tag(name = "Assessments", description = "Bewertungs-Workflow")
public class AssessmentsController {

    private final AssessmentWriteService writeService;

    public AssessmentsController(AssessmentWriteService writeService) {
        this.writeService = writeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CVM_ASSESSOR','CVM_ADMIN')")
    @Operation(summary = "Neuen Bewertungs-Vorschlag anlegen (Status PROPOSED)")
    public ResponseEntity<AssessmentResponse> anlegen(
            @Valid @RequestBody AssessmentRequest request) {
        FindingQueueView view = writeService.manualProposeView(
                new AssessmentWriteService.ManualProposeCommand(
                        request.findingId(),
                        request.ahsSeverity(),
                        request.rationale(),
                        request.sourceFields(),
                        request.decidedBy(),
                        request.productVersionId(),
                        request.environmentId()));
        return ResponseEntity
                .created(URI.create("/api/v1/assessments/" + view.assessmentId()))
                .body(AssessmentResponse.from(view));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('CVM_APPROVER','CVM_ADMIN')")
    @Operation(summary = "Assessment freigeben (Vier-Augen-Pruefung bei Downgrades)")
    public ResponseEntity<AssessmentResponse> freigeben(
            @PathVariable UUID id,
            @Valid @RequestBody AssessmentApproveRequest request) {
        AssessmentWriteService.MitigationInput mitigation = null;
        if (request.strategy() != null) {
            mitigation = new AssessmentWriteService.MitigationInput(
                    request.strategy(),
                    request.targetVersion(),
                    request.plannedFor(),
                    request.mitigationNotes());
        }
        FindingQueueView view = writeService.approveView(
                id, request.approverId(), mitigation, request.severity());
        return ResponseEntity.ok(AssessmentResponse.from(view));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('CVM_APPROVER','CVM_REVIEWER','CVM_ADMIN')")
    @Operation(summary = "Assessment ablehnen (Kommentar pflicht)")
    public ResponseEntity<AssessmentResponse> ablehnen(
            @PathVariable UUID id,
            @Valid @RequestBody AssessmentRejectRequest request) {
        FindingQueueView view = writeService.rejectView(id, request.approverId(), request.comment());
        return ResponseEntity.ok(AssessmentResponse.from(view));
    }
}
