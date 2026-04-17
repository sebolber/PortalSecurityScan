package com.ahs.cvm.application.cve;

import com.ahs.cvm.application.cve.CveEnrichmentPort.FeedEnrichment;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.cve.CveRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotente CVE-Anreicherung. Wird sowohl vom
 * {@code ScanIngestedEvent}-Listener als auch von Scheduled-Jobs und dem
 * Admin-Endpunkt aufgerufen.
 *
 * <p>Cache-Strategie: Re-Fetch nur, wenn {@code last_fetched_at} aelter als
 * {@code refreshAfter} ist (Default 24 h).
 */
@Service
public class CveEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(CveEnrichmentService.class);

    private static final Duration REFRESH_AFTER_DEFAULT = Duration.ofHours(24);

    private final CveRepository cveRepository;
    private final Map<String, CveEnrichmentPort> portsByName;
    private final Duration refreshAfter;

    public CveEnrichmentService(
            CveRepository cveRepository, List<CveEnrichmentPort> ports) {
        this(cveRepository, ports, REFRESH_AFTER_DEFAULT);
    }

    public CveEnrichmentService(
            CveRepository cveRepository,
            List<CveEnrichmentPort> ports,
            Duration refreshAfter) {
        this.cveRepository = cveRepository;
        this.refreshAfter = refreshAfter;
        Map<String, CveEnrichmentPort> byName = new HashMap<>();
        for (CveEnrichmentPort port : ports) {
            byName.put(port.feedName().toUpperCase(Locale.ROOT), port);
        }
        this.portsByName = Map.copyOf(byName);
    }

    @Transactional
    public Optional<Cve> enrich(String cveId) {
        Cve cve = cveRepository
                .findByCveId(cveId)
                .orElseGet(() -> cveRepository.save(
                        Cve.builder().cveId(cveId).source("STUB").build()));

        if (!istAbgelaufen(cve.getLastFetchedAt())) {
            log.debug("CVE {} frisch genug, kein Re-Fetch", cveId);
            return Optional.of(cve);
        }

        List<FeedEnrichment> ergebnisse = new ArrayList<>();
        for (CveEnrichmentPort port : portsByName.values()) {
            if (!port.isEnabled()) continue;
            try {
                port.fetch(cveId).ifPresent(ergebnisse::add);
            } catch (RuntimeException e) {
                log.warn("Feed {} fehlgeschlagen fuer {}: {}",
                        port.feedName(), cveId, e.getMessage());
            }
        }
        ergebnisse.forEach(e -> uebernehme(cve, e));
        cve.setLastFetchedAt(Instant.now());
        return Optional.of(cveRepository.save(cve));
    }

    @Transactional
    public int refreshAll(Optional<String> feedFilter) {
        int counter = 0;
        for (CveEnrichmentPort port : portsByName.values()) {
            if (feedFilter.isPresent()
                    && !feedFilter.get().equalsIgnoreCase(port.feedName())
                    && !"ALL".equalsIgnoreCase(feedFilter.get())) {
                continue;
            }
            if (!port.isEnabled()) continue;
            for (FeedEnrichment e : port.fetchAll()) {
                Cve cve = cveRepository
                        .findByCveId(e.cveId())
                        .orElseGet(() -> cveRepository.save(
                                Cve.builder().cveId(e.cveId()).source(port.feedName()).build()));
                uebernehme(cve, e);
                cve.setLastFetchedAt(Instant.now());
                cveRepository.save(cve);
                counter++;
            }
        }
        return counter;
    }

    private boolean istAbgelaufen(Instant lastFetchedAt) {
        return lastFetchedAt == null
                || lastFetchedAt.isBefore(Instant.now().minus(refreshAfter));
    }

    private void uebernehme(Cve cve, FeedEnrichment e) {
        if (e.summary() != null && cve.getSummary() == null) {
            cve.setSummary(e.summary());
        }
        if (e.cvssBaseScore() != null) {
            cve.setCvssBaseScore(e.cvssBaseScore());
        }
        if (e.cvssVector() != null) {
            cve.setCvssVector(e.cvssVector());
        }
        if (Boolean.TRUE.equals(e.kevListed())) {
            cve.setKevListed(Boolean.TRUE);
            cve.setKevListedAt(e.kevAddedAt());
        }
        if (e.epssScore() != null) {
            cve.setEpssScore(e.epssScore());
        }
        if (e.epssPercentile() != null) {
            cve.setEpssPercentile(e.epssPercentile());
        }
        if (e.cwes() != null && !e.cwes().isEmpty()) {
            cve.setCwes(e.cwes());
        }
        if (e.advisories() != null && !e.advisories().isEmpty()) {
            cve.setAdvisories(e.advisories());
        }
        String quelle = e.source();
        if (Objects.equals(cve.getSource(), "STUB") || cve.getSource() == null) {
            cve.setSource(quelle);
        }
    }

}
