package com.ahs.cvm.application.cve;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Port, den {@code cvm-integration} implementiert. Die
 * {@code cvm-application} kennt die konkreten Adapter nicht, nur die
 * normierten Anreicherungsdaten.
 *
 * <p>Das vermeidet eine Kopplung {@code application -> integration}.
 */
public interface CveEnrichmentPort {

    String feedName();

    boolean isEnabled();

    Optional<FeedEnrichment> fetch(String cveId);

    default List<FeedEnrichment> fetchAll() {
        return List.of();
    }

    /** Rein anwendungsseitige DTO-Repraesentation eines Anreicherungsergebnisses. */
    record FeedEnrichment(
            String cveId,
            String source,
            String summary,
            BigDecimal cvssBaseScore,
            String cvssVector,
            Boolean kevListed,
            Instant kevAddedAt,
            BigDecimal epssScore,
            BigDecimal epssPercentile,
            List<String> cwes,
            List<Map<String, Object>> advisories) {}
}
