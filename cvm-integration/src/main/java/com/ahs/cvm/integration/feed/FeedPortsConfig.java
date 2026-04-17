package com.ahs.cvm.integration.feed;

import com.ahs.cvm.application.cve.CveEnrichmentPort;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Erzeugt je {@link VulnerabilityFeedClient} einen
 * {@link CveEnrichmentPort}. Damit steht die Application-Schicht nur mit
 * der Port-Abstraktion in Kontakt.
 */
@Configuration
public class FeedPortsConfig {

    @Bean
    public List<CveEnrichmentPort> cveEnrichmentPorts(List<VulnerabilityFeedClient> clients) {
        return clients.stream().<CveEnrichmentPort>map(FeedClientAdapter::new).toList();
    }
}
