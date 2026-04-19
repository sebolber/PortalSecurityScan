package com.ahs.cvm.integration.ghsa;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
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

    @Test
    @DisplayName(
            "GHSA (Iteration 67): Token aus SystemParameterResolver "
                    + "wird pro Call gelesen und greift ohne Neustart")
    void parameterResolverOverrideGreiftOhneRestart() throws Exception {
        String body = new String(
                new ClassPathResource("fixtures/ghsa/cve-2017-18640.json")
                        .getContentAsByteArray());
        wireMock.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        FeedProperties properties = new FeedProperties();
        properties.getGhsa().setBaseUrl(
                "http://localhost:" + wireMock.port() + "/graphql");
        properties.getGhsa().setEnabled(true);
        properties.getGhsa().setRequestsPerWindow(100);
        properties.getGhsa().setWindowSeconds(1);
        properties.getGhsa().setApiKey("fallback-token");

        Map<String, String> store = new HashMap<>();
        store.put("cvm.feed.ghsa.api-key", "store-token-1");
        SystemParameterResolver resolver = Mockito.mock(SystemParameterResolver.class);
        Mockito.when(resolver.resolve("cvm.feed.ghsa.api-key"))
                .thenAnswer(inv -> Optional.ofNullable(store.get("cvm.feed.ghsa.api-key")));

        GhsaFeedClient resolverAware = new GhsaFeedClient(
                properties,
                new FeedRateLimiter(properties),
                new FeedMetrics(new SimpleMeterRegistry()),
                Optional.of(resolver));

        resolverAware.fetch("CVE-2017-18640");
        store.put("cvm.feed.ghsa.api-key", "store-token-2");
        resolverAware.fetch("CVE-2017-18640");

        List<com.github.tomakehurst.wiremock.verification.LoggedRequest> requests =
                wireMock.findAll(postRequestedFor(urlEqualTo("/graphql")));
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).getHeader("Authorization"))
                .isEqualTo("Bearer store-token-1");
        assertThat(requests.get(1).getHeader("Authorization"))
                .isEqualTo("Bearer store-token-2");
    }
}
