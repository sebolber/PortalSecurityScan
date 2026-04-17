package com.ahs.cvm.integration.feed;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Normalisierte Anreicherungsdaten, die ein Adapter fuer eine einzelne
 * CVE zurueckliefert. Felder sind {@code null}, wenn die jeweilige Quelle
 * dazu nichts aussagt.
 */
public record CveEnrichment(
        String cveId,
        FeedSource source,
        String summary,
        BigDecimal cvssBaseScore,
        String cvssVector,
        Boolean kevListed,
        Instant kevAddedAt,
        BigDecimal epssScore,
        BigDecimal epssPercentile,
        List<String> cwes,
        List<Map<String, Object>> advisories) {}
