package com.ahs.cvm.integration.git;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import com.ahs.cvm.integration.git.GitProviderPort.CommitSummary;
import com.ahs.cvm.integration.git.GitProviderPort.ReleaseNotes;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class GitHubApiProviderTest {

    private WireMockServer wiremock;
    private GitHubApiProvider provider;

    @BeforeEach
    void setUp() {
        wiremock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wiremock.start();
        RestClient rest = RestClient.builder()
                .baseUrl(wiremock.baseUrl())
                .requestFactory(new SimpleClientHttpRequestFactory())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        provider = new GitHubApiProvider(rest);
    }

    @AfterEach
    void tearDown() {
        wiremock.stop();
    }

    @Test
    @DisplayName("GitHub: releaseNotes parst body/html_url/published_at")
    void releaseNotesOk() {
        wiremock.stubFor(get(urlEqualTo("/repos/foo/bar/releases/tags/v1.2.3"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "body":"Fix CVE-2025-48924 in Parser.",
                                  "html_url":"https://github.com/foo/bar/releases/tag/v1.2.3",
                                  "published_at":"2026-03-01T10:00:00Z"
                                }""")));

        Optional<ReleaseNotes> notes = provider.releaseNotes(
                "https://github.com/foo/bar", "v1.2.3");

        assertThat(notes).isPresent();
        assertThat(notes.get().body()).contains("CVE-2025-48924");
        assertThat(notes.get().url()).endsWith("/v1.2.3");
    }

    @Test
    @DisplayName("GitHub: compare liefert Commits mit Globalem File-Array")
    void compareOk() {
        wiremock.stubFor(get(urlEqualTo("/repos/foo/bar/compare/v1.2.2...v1.2.3"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "commits":[
                                    {"sha":"abc","commit":{
                                       "message":"fix(parser): guard against XXE",
                                       "author":{"email":"dev@foo","date":"2026-02-28T09:00:00Z"}},
                                     "html_url":"https://github.com/foo/bar/commit/abc"},
                                    {"sha":"def","commit":{
                                       "message":"chore: bump deps",
                                       "author":{"email":"dev@foo","date":"2026-02-27T09:00:00Z"}},
                                     "html_url":"https://github.com/foo/bar/commit/def"}
                                  ],
                                  "files":[
                                    {"filename":"src/Parser.java"},
                                    {"filename":"pom.xml"}
                                  ]
                                }""")));

        List<CommitSummary> commits = provider.compare(
                "https://github.com/foo/bar", "v1.2.2", "v1.2.3");

        assertThat(commits).hasSize(2);
        assertThat(commits.get(0).sha()).isEqualTo("abc");
        assertThat(commits.get(0).filesTouched()).contains("src/Parser.java");
    }

    @Test
    @DisplayName("GitHub: 404 -> Optional.empty (kein Crash)")
    void notFound() {
        wiremock.stubFor(get(urlEqualTo("/repos/foo/bar/releases/tags/missing"))
                .willReturn(aResponse().withStatus(404).withBody("not found")));

        assertThat(provider.releaseNotes("https://github.com/foo/bar", "missing"))
                .isEmpty();
    }

    @Test
    @DisplayName("GitHub: compare 404 -> leere Liste")
    void compareNotFound() {
        wiremock.stubFor(get(urlEqualTo("/repos/foo/bar/compare/x...y"))
                .willReturn(aResponse().withStatus(404).withBody("not found")));

        assertThat(provider.compare("https://github.com/foo/bar", "x", "y"))
                .isEmpty();
    }

    @Test
    @DisplayName("GitHub: slugOf extrahiert owner/repo aus verschiedenen URL-Formaten")
    void slug() {
        assertThat(GitHubApiProvider.slugOf("https://github.com/foo/bar")).isEqualTo("foo/bar");
        assertThat(GitHubApiProvider.slugOf("https://github.com/foo/bar.git")).isEqualTo("foo/bar");
        assertThat(GitHubApiProvider.slugOf("git@github.com:foo/bar.git")).isEqualTo("foo/bar");
        assertThat(GitHubApiProvider.slugOf("https://example.com/foo/bar")).isNull();
    }

    @Test
    @DisplayName(
            "GitHub (Iteration 68): Token aus SystemParameterResolver "
                    + "wird pro Call gelesen und greift ohne Neustart")
    void tokenOverrideGreiftOhneRestart() {
        wiremock.stubFor(get(urlEqualTo("/repos/foo/bar/releases/tags/v1.2.3"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "body":"fix",
                                  "html_url":"https://github.com/foo/bar/releases/tag/v1.2.3"
                                }""")));

        Map<String, String> store = new HashMap<>();
        store.put("cvm.ai.fix-verification.github.token", "store-token-1");
        SystemParameterResolver resolver = Mockito.mock(SystemParameterResolver.class);
        Mockito.when(resolver.resolve("cvm.ai.fix-verification.github.token"))
                .thenAnswer(inv -> Optional.ofNullable(
                        store.get("cvm.ai.fix-verification.github.token")));

        RestClient rest = RestClient.builder()
                .baseUrl(wiremock.baseUrl())
                .requestFactory(new SimpleClientHttpRequestFactory())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        GitHubApiProvider resolverAware = new GitHubApiProvider(
                rest, "fallback-token", Optional.of(resolver));

        resolverAware.releaseNotes("https://github.com/foo/bar", "v1.2.3");
        store.put("cvm.ai.fix-verification.github.token", "store-token-2");
        resolverAware.releaseNotes("https://github.com/foo/bar", "v1.2.3");

        List<com.github.tomakehurst.wiremock.verification.LoggedRequest> requests =
                wiremock.findAll(
                        getRequestedFor(urlEqualTo("/repos/foo/bar/releases/tags/v1.2.3")));
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).getHeader("Authorization"))
                .isEqualTo("Bearer store-token-1");
        assertThat(requests.get(1).getHeader("Authorization"))
                .isEqualTo("Bearer store-token-2");
    }
}
