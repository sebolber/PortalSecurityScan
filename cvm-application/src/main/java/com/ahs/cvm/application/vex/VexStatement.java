package com.ahs.cvm.application.vex;

import com.ahs.cvm.domain.enums.AhsSeverity;
import java.util.List;

/**
 * Interne Darstellung einer VEX-Aussage (Iteration 20).
 *
 * @param cveKey           z.&nbsp;B. {@code CVE-2025-48924}.
 * @param productPurl      PURL der betroffenen Komponente.
 * @param status           VEX-Status.
 * @param justification    Justification-Code (z.&nbsp;B.
 *     {@code component_not_present}).
 * @param detail           Detail-Text (rationale).
 * @param severity         urspruengliche Bewertung.
 * @param addressedByCommits Liste von Fix-Commit-URLs (optional).
 */
public record VexStatement(
        String cveKey,
        String productPurl,
        VexStatus status,
        String justification,
        String detail,
        AhsSeverity severity,
        List<String> addressedByCommits) {

    public VexStatement {
        addressedByCommits = addressedByCommits == null
                ? List.of() : List.copyOf(addressedByCommits);
    }
}
