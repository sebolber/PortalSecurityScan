package com.ahs.cvm.integration.nvd;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import com.ahs.cvm.integration.feed.CveEnrichment;
import com.ahs.cvm.integration.feed.FeedMetrics;
import com.ahs.cvm.integration.feed.FeedProperties;
import com.ahs.cvm.integration.feed.FeedRateLimiter;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

class NvdFeedClientTest {

    private WireMockServer wireMock;
    private NvdFeedClient client;

    @BeforeEach
    void start() {
        wireMock = new WireMockServer(com.github.tomakehurst.wiremock.core.WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        FeedProperties properties = new FeedProperties();
        properties.getNvd().setBaseUrl("http://localhost:" + wireMock.port());
        properties.getNvd().setEnabled(true);
        properties.getNvd().setRequestsPerWindow(100);
        properties.getNvd().setWindowSeconds(1);
        FeedRateLimiter limiter = new FeedRateLimiter(properties);
        FeedMetrics metrics = new FeedMetrics(new SimpleMeterRegistry());

        client = new NvdFeedClient(properties, limiter, metrics);
    }

    @AfterEach
    void stop() {
        wireMock.stop();
    }

    @Test
    @DisplayName("NVD: liest Score 7.5 und CWE-776 aus Beispiel-Response")
    void fetchHappyPath() throws Exception {
        String body = new String(
                new ClassPathResource("fixtures/nvd/cve-2017-18640.json")
                        .getContentAsByteArray());
        wireMock.stubFor(get(urlPathEqualTo("/rest/json/cves/2.0"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

        Optional<CveEnrichment> result = client.fetch("CVE-2017-18640");

        assertThat(result).isPresent();
        CveEnrichment e = result.get();
        assertThat(e.cvssBaseScore().doubleValue()).isEqualTo(7.5);
        assertThat(e.cvssVector()).contains("AV:N");
        assertThat(e.cwes()).containsExactly("CWE-776");
        assertThat(e.summary()).contains("Billion Laughs");
    }

    @Test
    @DisplayName("NVD: wirft FeedClientException bei HTTP 500")
    void fetchWirftBeiFehler() {
        wireMock.stubFor(get(urlPathEqualTo("/rest/json/cves/2.0"))
                .willReturn(aResponse().withStatus(500)));

        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                        com.ahs.cvm.integration.feed.FeedClientException.class,
                        () -> client.fetch("CVE-2017-18640"))
                .source())
                .isEqualTo(com.ahs.cvm.integration.feed.FeedSource.NVD);
    }

    @Test
    @DisplayName("NVD: leere Ergebnisliste liefert Optional.empty")
    void fetchLeereErgebnisse() {
        wireMock.stubFor(get(urlPathEqualTo("/rest/json/cves/2.0"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"vulnerabilities\":[]}")));

        Optional<CveEnrichment> result = client.fetch("CVE-0000-0000");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName(
            "NVD (Iteration 67): api-key aus dem SystemParameterResolver "
                    + "wird pro Call gelesen und greift ohne Neustart")
    void parameterResolverOverrideGreiftOhneRestart() throws Exception {
        String body = new String(
                new ClassPathResource("fixtures/nvd/cve-2017-18640.json")
                        .getContentAsByteArray());
        wireMock.stubFor(get(urlPathEqualTo("/rest/json/cves/2.0"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        FeedProperties properties = new FeedProperties();
        properties.getNvd().setBaseUrl("http://localhost:" + wireMock.port());
        properties.getNvd().setEnabled(true);
        properties.getNvd().setRequestsPerWindow(100);
        properties.getNvd().setWindowSeconds(1);
        properties.getNvd().setApiKey("fallback-key");

        Map<String, String> store = new HashMap<>();
        store.put("cvm.feed.nvd.api-key", "store-key-1");
        SystemParameterResolver resolver = Mockito.mock(SystemParameterResolver.class);
        Mockito.when(resolver.resolve("cvm.feed.nvd.api-key"))
                .thenAnswer(inv -> Optional.ofNullable(store.get("cvm.feed.nvd.api-key")));

        NvdFeedClient resolverAware = new NvdFeedClient(
                properties,
                new FeedRateLimiter(properties),
                new FeedMetrics(new SimpleMeterRegistry()),
                Optional.of(resolver));

        resolverAware.fetch("CVE-2017-18640");
        // Zweiter Call nach Store-Update.
        store.put("cvm.feed.nvd.api-key", "store-key-2");
        resolverAware.fetch("CVE-2017-18640");

        List<com.github.tomakehurst.wiremock.verification.LoggedRequest> requests =
                wireMock.findAll(getRequestedFor(urlPathEqualTo("/rest/json/cves/2.0")));
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).getHeader("apiKey")).isEqualTo("store-key-1");
        assertThat(requests.get(1).getHeader("apiKey")).isEqualTo("store-key-2");
    }
}
