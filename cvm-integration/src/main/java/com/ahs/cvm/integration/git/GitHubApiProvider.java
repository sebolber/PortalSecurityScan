package com.ahs.cvm.integration.git;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP-Adapter fuer die GitHub-REST-API (Releases + Compare).
 * Aktiv nur bei {@code cvm.ai.fix-verification.github.enabled=true}.
 *
 * <p>Bewusst nur zwei Endpunkte:
 * <ul>
 *   <li>{@code GET /repos/{owner}/{repo}/releases/tags/{tag}}</li>
 *   <li>{@code GET /repos/{owner}/{repo}/compare/{from}...{to}}</li>
 * </ul>
 *
 * <p>Iteration 68 (CVM-305): Der GitHub-Token wird pro Call aus
 * dem {@link SystemParameterResolver} aufgeloest
 * ({@code cvm.ai.fix-verification.github.token}). Aenderungen im
 * Admin-UI greifen ohne Neustart. Der {@code @Value}-Wert dient
 * weiterhin als Fallback.
 */
@Component("githubApiProvider")
@ConditionalOnProperty(prefix = "cvm.ai.fix-verification.github",
        name = "enabled", havingValue = "true")
public class GitHubApiProvider implements GitProviderPort {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiProvider.class);
    private static final String PARAM_TOKEN = "cvm.ai.fix-verification.github.token";

    private final RestClient restClient;
    private final String fallbackToken;
    private final Optional<SystemParameterResolver> parameterResolver;

    @Autowired
    public GitHubApiProvider(
            @Value("${cvm.ai.fix-verification.github.base-url:https://api.github.com}") String baseUrl,
            @Value("${cvm.ai.fix-verification.github.token:${GITHUB_TOKEN:}}") String token,
            Optional<SystemParameterResolver> parameterResolver) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.fallbackToken = token == null ? "" : token;
        this.parameterResolver = parameterResolver == null
                ? Optional.empty()
                : parameterResolver;
    }

    /** Test-Konstruktor (WireMock) ohne Parameter-Resolver. */
    public GitHubApiProvider(RestClient restClient) {
        this(restClient, "", Optional.empty());
    }

    /** Test-Konstruktor (WireMock) mit optionalem Parameter-Resolver. */
    public GitHubApiProvider(
            RestClient restClient,
            String fallbackToken,
            Optional<SystemParameterResolver> parameterResolver) {
        this.restClient = restClient;
        this.fallbackToken = fallbackToken == null ? "" : fallbackToken;
        this.parameterResolver = parameterResolver == null
                ? Optional.empty()
                : parameterResolver;
    }

    @Override
    public Optional<ReleaseNotes> releaseNotes(String repoUrl, String tag) {
        String slug = slugOf(repoUrl);
        if (slug == null) {
            return Optional.empty();
        }
        String token = resolveToken();
        try {
            JsonNode body = restClient.get()
                    .uri("/repos/" + slug + "/releases/tags/" + tag)
                    .headers(h -> authorize(h, token))
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null) {
                return Optional.empty();
            }
            return Optional.of(new ReleaseNotes(
                    repoUrl,
                    tag,
                    body.path("body").asText(""),
                    parseInstant(body.path("published_at").asText(null)),
                    body.path("html_url").asText(repoUrl)));
        } catch (RuntimeException ex) {
            log.debug("GitHub-Release-Notes {}#{} nicht erreichbar: {}",
                    slug, tag, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<CommitSummary> compare(String repoUrl, String fromTag, String toTag) {
        String slug = slugOf(repoUrl);
        if (slug == null) {
            return List.of();
        }
        String token = resolveToken();
        try {
            JsonNode body = restClient.get()
                    .uri("/repos/" + slug + "/compare/" + fromTag + "..." + toTag)
                    .headers(h -> authorize(h, token))
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null || !body.has("commits")) {
                return List.of();
            }
            List<CommitSummary> result = new ArrayList<>();
            for (JsonNode c : body.path("commits")) {
                List<String> files = new ArrayList<>();
                // Compare-API liefert Files nur auf Root-Level, nicht pro Commit.
                result.add(new CommitSummary(
                        c.path("sha").asText(""),
                        c.path("commit").path("message").asText(""),
                        c.path("html_url").asText(repoUrl),
                        c.path("commit").path("author").path("email").asText(
                                c.path("author").path("login").asText("")),
                        parseInstant(c.path("commit").path("author").path("date").asText(null)),
                        files));
            }
            if (body.has("files")) {
                // File-Liste auf Range-Ebene separat mitfuehren, weil Compare-API sie
                // nicht pro Commit liefert. Wir haengen sie allen Commits gleich an.
                List<String> globalFiles = new ArrayList<>();
                for (JsonNode f : body.path("files")) {
                    globalFiles.add(f.path("filename").asText(""));
                }
                List<CommitSummary> enriched = new ArrayList<>(result.size());
                for (CommitSummary c : result) {
                    enriched.add(new CommitSummary(
                            c.sha(), c.message(), c.url(), c.author(), c.date(),
                            globalFiles));
                }
                return List.copyOf(enriched);
            }
            return List.copyOf(result);
        } catch (RuntimeException ex) {
            log.debug("GitHub-Compare {}#{}...{} nicht erreichbar: {}",
                    slug, fromTag, toTag, ex.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean postMergeRequestComment(String repoUrl,
            String mergeRequestId, String body) {
        String slug = slugOf(repoUrl);
        if (slug == null || mergeRequestId == null || mergeRequestId.isBlank()
                || body == null || body.isBlank()) {
            return false;
        }
        String token = resolveToken();
        try {
            restClient.post()
                    .uri("/repos/" + slug + "/issues/" + mergeRequestId.trim()
                            + "/comments")
                    .headers(h -> authorize(h, token))
                    .body(java.util.Map.of("body", body))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RuntimeException ex) {
            log.debug("GitHub-MR-Kommentar {}#{} fehlgeschlagen: {}",
                    slug, mergeRequestId, ex.getMessage());
            return false;
        }
    }

    private String resolveToken() {
        return parameterResolver
                .flatMap(r -> r.resolve(PARAM_TOKEN))
                .filter(v -> !v.isBlank())
                .orElse(fallbackToken);
    }

    private void authorize(HttpHeaders headers, String token) {
        if (token != null && !token.isBlank()) {
            headers.set("Authorization", "Bearer " + token);
        }
    }

    static String slugOf(String repoUrl) {
        if (repoUrl == null) {
            return null;
        }
        String trimmed = repoUrl.trim().replaceFirst("\\.git$", "");
        int gh = trimmed.indexOf("github.com");
        if (gh < 0) {
            return null;
        }
        String rest = trimmed.substring(gh + "github.com".length());
        if (rest.startsWith("/") || rest.startsWith(":")) {
            rest = rest.substring(1);
        }
        if (rest.isBlank()) {
            return null;
        }
        return rest;
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank() || "null".equals(iso)) {
            return null;
        }
        try {
            return Instant.parse(iso);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
