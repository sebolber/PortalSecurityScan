package com.ahs.cvm.integration.git;

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
 */
@Component("githubApiProvider")
@ConditionalOnProperty(prefix = "cvm.ai.fix-verification.github",
        name = "enabled", havingValue = "true")
public class GitHubApiProvider implements GitProviderPort {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiProvider.class);

    private final RestClient restClient;

    @Autowired
    public GitHubApiProvider(
            @Value("${cvm.ai.fix-verification.github.base-url:https://api.github.com}") String baseUrl,
            @Value("${cvm.ai.fix-verification.github.token:${GITHUB_TOKEN:}}") String token) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (token != null && !token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }
        this.restClient = builder.build();
    }

    /** Test-Konstruktor (WireMock). */
    public GitHubApiProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<ReleaseNotes> releaseNotes(String repoUrl, String tag) {
        String slug = slugOf(repoUrl);
        if (slug == null) {
            return Optional.empty();
        }
        try {
            JsonNode body = restClient.get()
                    .uri("/repos/" + slug + "/releases/tags/" + tag)
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
        try {
            JsonNode body = restClient.get()
                    .uri("/repos/" + slug + "/compare/" + fromTag + "..." + toTag)
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
        try {
            restClient.post()
                    .uri("/repos/" + slug + "/issues/" + mergeRequestId.trim()
                            + "/comments")
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
