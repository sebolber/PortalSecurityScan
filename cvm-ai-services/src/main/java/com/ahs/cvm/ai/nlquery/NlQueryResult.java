package com.ahs.cvm.ai.nlquery;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Antwort auf eine NL-Query (Iteration 19, CVM-50). */
public record NlQueryResult(
        NlFilter filter,
        String explanation,
        List<Row> results,
        List<String> rejectedReasons) {

    public NlQueryResult {
        results = results == null ? List.of() : List.copyOf(results);
        rejectedReasons = rejectedReasons == null
                ? List.of() : List.copyOf(rejectedReasons);
    }

    public record Row(
            UUID assessmentId,
            String cveKey,
            AhsSeverity severity,
            AssessmentStatus status,
            String environmentKey,
            Instant createdAt) {}
}
