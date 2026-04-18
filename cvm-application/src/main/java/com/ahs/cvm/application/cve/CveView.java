package com.ahs.cvm.application.cve;

import com.ahs.cvm.persistence.cve.Cve;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-View einer CVE fuer die Inventar-UI (Iteration 26, CVM-57).
 */
public record CveView(
        UUID id,
        String cveId,
        String summary,
        BigDecimal cvssBaseScore,
        String cvssVector,
        boolean kevListed,
        BigDecimal epssScore,
        BigDecimal epssPercentile,
        List<String> cwes,
        Instant publishedAt,
        Instant lastModifiedAt,
        String source) {

    public static CveView from(Cve c) {
        return new CveView(
                c.getId(),
                c.getCveId(),
                c.getSummary(),
                c.getCvssBaseScore(),
                c.getCvssVector(),
                Boolean.TRUE.equals(c.getKevListed()),
                c.getEpssScore(),
                c.getEpssPercentile(),
                c.getCwes() == null ? List.of() : List.copyOf(c.getCwes()),
                c.getPublishedAt(),
                c.getLastModifiedAt(),
                c.getSource());
    }
}
