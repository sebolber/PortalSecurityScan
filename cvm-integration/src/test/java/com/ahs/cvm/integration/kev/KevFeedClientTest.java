package com.ahs.cvm.integration.kev;

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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class KevFeedClientTest {

    private WireMockServer wireMock;
    private KevFeedClient client;

    @BeforeEach
    void start() {
        wireMock = new WireMockServer(com.github.tomakehurst.wiremock.core.WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        FeedProperties properties = new FeedProperties();
        properties.getKev().setBaseUrl(
                "http://localhost:" + wireMock.port() + "/kev.json");
        properties.getKev().setEnabled(true);
        properties.getKev().setRequestsPerWindow(100);
        properties.getKev().setWindowSeconds(1);

        client = new KevFeedClient(
                properties,
                new FeedRateLimiter(properties),
                new FeedMetrics(new SimpleMeterRegistry()));
    }

    @AfterEach
    void stop() {
        wireMock.stop();
    }

    @Test
    @DisplayName("KEV-Feed: CVE wird als kevListed markiert, wenn in letztem Dump enthalten")
    void kevFeedMarkiertBekannteCves() throws Exception {
        String body = new String(
                new ClassPathResource("fixtures/kev/dump.json")
                        .getContentAsByteArray());
        wireMock.stubFor(get(urlPathEqualTo("/kev.json"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

        List<CveEnrichment> alle = client.fetchAll();

        assertThat(alle).hasSize(2);
        Optional<CveEnrichment> snakeyaml = client.fetch("CVE-2017-18640");
        assertThat(snakeyaml).isPresent();
        assertThat(snakeyaml.get().kevListed()).isTrue();
        assertThat(snakeyaml.get().kevAddedAt()).isNotNull();
    }

    @Test
    @DisplayName("KEV-Feed: unbekannte CVE liefert Optional.empty aus Cache")
    void kevFeedUnbekannt() throws Exception {
        String body = new String(
                new ClassPathResource("fixtures/kev/dump.json")
                        .getContentAsByteArray());
        wireMock.stubFor(get(urlPathEqualTo("/kev.json"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
        client.fetchAll();

        assertThat(client.fetch("CVE-0000-0000")).isEmpty();
    }
}
