package com.ahs.cvm.api.assessment;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.MitigationStrategy;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * Body fuer {@code POST /api/v1/assessments/{id}/approve}.
 *
 * <p>Iteration 61 (CVM-62): optionales Feld {@code severity}. Wenn gesetzt,
 * uebernimmt die Freigabe diese neue Severity statt der urspruenglichen
 * Vorschlags-Severity. Die Vier-Augen-Pruefung erfolgt gegen die
 * resultierende Severity - ein Downgrade auf {@code NOT_APPLICABLE} oder
 * {@code INFORMATIONAL} durch den eigenen Autor wird weiter abgewiesen.
 */
public record AssessmentApproveRequest(
        @NotBlank String approverId,
        AhsSeverity severity,
        MitigationStrategy strategy,
        String targetVersion,
        Instant plannedFor,
        String mitigationNotes) {}
