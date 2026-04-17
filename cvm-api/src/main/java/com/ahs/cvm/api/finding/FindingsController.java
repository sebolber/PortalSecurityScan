package com.ahs.cvm.api.finding;

import com.ahs.cvm.api.assessment.AssessmentResponse;
import com.ahs.cvm.application.assessment.AssessmentQueueService;
import com.ahs.cvm.application.assessment.AssessmentQueueService.QueueFilter;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Endpunkt fuer die Bewertungs-Queue.
 *
 * <p>{@code GET /api/v1/findings} listet offene Vorschlaege. Per Default
 * werden PROPOSED und NEEDS_REVIEW zurueckgegeben; mit
 * {@code ?status=PROPOSED} kann auf eine Stufe gefiltert werden.
 */
@RestController
@RequestMapping("/api/v1/findings")
@Tag(name = "Findings-Queue", description = "Offene Bewertungsvorschlaege")
public class FindingsController {

    private final AssessmentQueueService queueService;

    public FindingsController(AssessmentQueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping
    @Operation(summary = "Bewertungs-Queue abfragen (Default: PROPOSED + NEEDS_REVIEW)")
    public ResponseEntity<List<AssessmentResponse>> queue(
            @RequestParam(value = "status", required = false) AssessmentStatus status,
            @RequestParam(value = "productVersionId", required = false) UUID productVersionId,
            @RequestParam(value = "environmentId", required = false) UUID environmentId,
            @RequestParam(value = "source", required = false) ProposalSource source) {
        List<AssessmentResponse> response = queueService
                .findeOffene(new QueueFilter(status, environmentId, productVersionId, source))
                .stream()
                .map(AssessmentResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
