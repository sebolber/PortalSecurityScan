package com.ahs.cvm.integration.feed;

import com.ahs.cvm.application.cve.CveEnrichmentPort;
import java.util.List;
import java.util.Optional;

/**
 * Brueckenklasse: macht aus einem {@link VulnerabilityFeedClient} einen
 * {@link CveEnrichmentPort}. Damit kennt die Application-Schicht nur noch
 * den neutralen Port, nicht das Integrations-Interface.
 */
public class FeedClientAdapter implements CveEnrichmentPort {

    private final VulnerabilityFeedClient delegate;

    public FeedClientAdapter(VulnerabilityFeedClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public String feedName() {
        return delegate.source().name();
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public Optional<FeedEnrichment> fetch(String cveId) {
        return delegate.fetch(cveId).map(FeedClientAdapter::map);
    }

    @Override
    public List<FeedEnrichment> fetchAll() {
        return delegate.fetchAll().stream().map(FeedClientAdapter::map).toList();
    }

    private static FeedEnrichment map(CveEnrichment e) {
        return new FeedEnrichment(
                e.cveId(),
                e.source().name(),
                e.summary(),
                e.cvssBaseScore(),
                e.cvssVector(),
                e.kevListed(),
                e.kevAddedAt(),
                e.epssScore(),
                e.epssPercentile(),
                e.cwes(),
                e.advisories());
    }
}
