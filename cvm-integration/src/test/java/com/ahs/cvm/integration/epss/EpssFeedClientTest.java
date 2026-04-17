package com.ahs.cvm.integration.epss;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.integration.feed.CveEnrichment;
import com.ahs.cvm.integration.feed.FeedMetrics;
import com.ahs.cvm.integration.feed.FeedProperties;
import com.ahs.cvm.integration.feed.FeedRateLimiter;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class EpssFeedClientTest {

    private WireMockServer wireMock;
    private EpssFeedClient client;

    @BeforeEach
    void start() {
        wireMock = new WireMockServer(com.github.tomakehurst.wiremock.core.WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        FeedProperties properties = new FeedProperties();
        properties.getEpss().setBaseUrl("http://localhost:" + wireMock.port() + "/epss");
        properties.getEpss().setEnabled(true);
        properties.getEpss().setRequestsPerWindow(100);
        properties.getEpss().setWindowSeconds(1);

        client = new EpssFeedClient(
                properties,
                new FeedRateLimiter(properties),
                new FeedMetrics(new SimpleMeterRegistry()));
    }

    @AfterEach
    void stop() {
        wireMock.stop();
    }

    @Test
    @DisplayName("EPSS: Score liegt im Bereich [0,1] und wird auf 4 Dezimalstellen normiert")
    void epssWirdKorrektGelesen() throws Exception {
        String body = new String(
                new ClassPathResource("fixtures/epss/cve-2017-18640.json")
                        .getContentAsByteArray());
        wireMock.stubFor(get(urlPathEqualTo("/epss"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

        Optional<CveEnrichment> result = client.fetch("CVE-2017-18640");

        assertThat(result).isPresent();
        CveEnrichment e = result.get();
        assertThat(e.epssScore()).isEqualByComparingTo(new BigDecimal("0.4321"));
        assertThat(e.epssPercentile()).isEqualByComparingTo(new BigDecimal("0.9876"));
    }
}
