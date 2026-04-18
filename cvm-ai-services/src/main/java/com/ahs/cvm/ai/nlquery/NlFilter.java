package com.ahs.cvm.ai.nlquery;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import java.util.List;

/**
 * Deterministischer, vom Validator geprüfter Filter fuer die
 * NL-Query (Iteration 19, CVM-50). KEIN SQL-Feld - nur fachliche
 * Whitelist.
 *
 * @param environmentKey       z.&nbsp;B. {@code REF-TEST} oder {@code PROD}.
 * @param productVersionLabel  {@code PortalCore-Test 1.14.2-test}.
 * @param severityIn           Pflicht-Liste (leer = keine Einschraenkung).
 * @param statusIn             Pflicht-Liste (leer = keine Einschraenkung).
 * @param minAgeDays           Optional - Minimalalter in Tagen.
 * @param hasUpstreamFix       Optional - filtert auf
 *     {@code finding.fixedInVersion != null}.
 * @param kevOnly              Optional - nur KEV-gelistete CVEs.
 * @param sortBy               Enum; Default {@link SortBy#AGE_DESC}.
 */
public record NlFilter(
        String environmentKey,
        String productVersionLabel,
        List<AhsSeverity> severityIn,
        List<AssessmentStatus> statusIn,
        Integer minAgeDays,
        Boolean hasUpstreamFix,
        Boolean kevOnly,
        SortBy sortBy) {

    public NlFilter {
        severityIn = severityIn == null ? List.of() : List.copyOf(severityIn);
        statusIn = statusIn == null ? List.of() : List.copyOf(statusIn);
        if (sortBy == null) {
            sortBy = SortBy.AGE_DESC;
        }
    }

    public enum SortBy { AGE_DESC, AGE_ASC, SEVERITY_DESC, SEVERITY_ASC }
}
