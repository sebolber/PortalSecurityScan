package com.ahs.cvm.integration.ghsa;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.integration.feed.CveEnrichment;
import com.ahs.cvm.integration.feed.FeedMetrics;
import com.ahs.cvm.integration.feed.FeedProperties;
import com.ahs.cvm.integration.feed.FeedRateLimiter;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class GhsaFeedClientTest {

    private WireMockServer wireMock;
    private GhsaFeedClient client;

    @BeforeEach
    void start() {
        wireMock = new WireMockServer(com.github.tomakehurst.wiremock.core.WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        FeedProperties properties = new FeedProperties();
        properties.getGhsa().setBaseUrl("http://localhost:" + wireMock.port() + "/graphql");
        properties.getGhsa().setApiKey("dummy-token");
        properties.getGhsa().setEnabled(true);
        properties.getGhsa().setRequestsPerWindow(100);
        properties.getGhsa().setWindowSeconds(1);

        client = new GhsaFeedClient(
                properties,
                new FeedRateLimiter(properties),
                new FeedMetrics(new SimpleMeterRegistry()));
    }

    @AfterEach
    void stop() {
        wireMock.stop();
    }

    @Test
    @DisplayName("GHSA: erkennt Advisory zu einer CVE mit Referenzen")
    void ghsaGraphqlHappyPath() throws Exception {
        String body = new String(
                new ClassPathResource("fixtures/ghsa/cve-2017-18640.json")
                        .getContentAsByteArray());
        wireMock.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

        Optional<CveEnrichment> result = client.fetch("CVE-2017-18640");

        assertThat(result).isPresent();
        assertThat(result.get().advisories()).hasSize(1);
        assertThat(result.get().advisories().get(0)).containsEntry("ghsaId", "GHSA-rvg8-pwq2-xj7q");
    }

    @Test
    @DisplayName("GHSA: ist ohne Token deaktiviert")
    void ghsaOhneTokenDeaktiviert() {
        FeedProperties properties = new FeedProperties();
        properties.getGhsa().setApiKey("");
        properties.getGhsa().setEnabled(true);
        properties.getGhsa().setRequestsPerWindow(100);
        properties.getGhsa().setWindowSeconds(1);

        GhsaFeedClient stummer = new GhsaFeedClient(
                properties,
                new FeedRateLimiter(properties),
                new FeedMetrics(new SimpleMeterRegistry()));

        assertThat(stummer.isEnabled()).isFalse();
        assertThat(stummer.fetch("CVE-2017-18640")).isEmpty();
    }
}
