package com.ahs.cvm.integration.osv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OsvComponentLookupTest {

    private OsvProperties defaultsEnabled() {
        OsvProperties p = new OsvProperties();
        p.setEnabled(true);
        p.setBaseUrl("https://osv.test");
        p.setBatchSize(500);
        p.setTimeoutMs(5_000);
        return p;
    }

    @Test
    @DisplayName("isEnabled: wird durch enabled und gueltige baseUrl gesteuert")
    void enabledFlag() {
        OsvProperties p = defaultsEnabled();
        p.setEnabled(false);
        assertThat(new OsvComponentLookup(p, RestClient.builder()).isEnabled())
                .isFalse();

        OsvProperties p2 = defaultsEnabled();
        p2.setBaseUrl("");
        assertThat(new OsvComponentLookup(p2, RestClient.builder()).isEnabled())
                .isFalse();

        OsvProperties p3 = defaultsEnabled();
        assertThat(new OsvComponentLookup(p3, RestClient.builder()).isEnabled())
                .isTrue();
    }

    @Test
    @DisplayName("Happy Path: Batch-Response liefert CVE-IDs pro PURL")
    void happyPath() {
        OsvProperties p = defaultsEnabled();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OsvComponentLookup lookup = new OsvComponentLookup(p, builder);

        String response = """
                {
                  "results": [
                    { "vulns": [
                        { "id": "GHSA-xx", "aliases": ["CVE-2024-1"] }
                      ] },
                    { "vulns": [
                        { "id": "CVE-2024-2" }
                      ] }
                  ]
                }
                """;
        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        Map<String, List<String>> result = lookup.findCveIdsForPurls(List.of(
                "pkg:npm/axios@1.6.7",
                "pkg:maven/org.example/foo@1.0.0"));

        assertThat(result).containsEntry(
                "pkg:npm/axios@1.6.7", List.of("CVE-2024-1"));
        assertThat(result).containsEntry(
                "pkg:maven/org.example/foo@1.0.0", List.of("CVE-2024-2"));
        server.verify();
    }

    @Test
    @DisplayName("Leere Response: keine Eintraege in der Map")
    void emptyResponse() {
        OsvProperties p = defaultsEnabled();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OsvComponentLookup lookup = new OsvComponentLookup(p, builder);

        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andRespond(withSuccess("{\"results\":[{}]}",
                        MediaType.APPLICATION_JSON));

        Map<String, List<String>> result = lookup.findCveIdsForPurls(List.of(
                "pkg:npm/ok@1.0.0"));

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("5xx-Fehler liefert leere Map, kein throw")
    void serverError() {
        OsvProperties p = defaultsEnabled();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OsvComponentLookup lookup = new OsvComponentLookup(p, builder);

        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andRespond(withServerError());

        Map<String, List<String>> result = lookup.findCveIdsForPurls(List.of(
                "pkg:npm/axios@1.6.7"));

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("Detail-Fallback: leere Aliase loest CVE-IDs via /v1/vulns/<id>")
    void detailFallbackOhneAliaseInBatch() {
        OsvProperties p = defaultsEnabled();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OsvComponentLookup lookup = new OsvComponentLookup(p, builder);

        // 1) Batch-Response liefert nur die GHSA-ID, keine Aliase
        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "results": [
                            { "vulns": [ { "id": "GHSA-3p68-rc4w-qgx5" } ] }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        // 2) Detail-Call liefert die Aliase
        server.expect(requestTo("https://osv.test/v1/vulns/GHSA-3p68-rc4w-qgx5"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "GHSA-3p68-rc4w-qgx5",
                          "aliases": ["CVE-2024-28849"]
                        }
                        """, MediaType.APPLICATION_JSON));

        Map<String, List<String>> r = lookup.findCveIdsForPurls(List.of(
                "pkg:npm/follow-redirects@1.15.5"));

        assertThat(r.get("pkg:npm/follow-redirects@1.15.5"))
                .containsExactly("CVE-2024-28849");
        server.verify();
    }

    @Test
    @DisplayName("Detail-Fallback: wird pro Advisory nur EINMAL aufgerufen (Cache)")
    void detailFallbackCached() {
        OsvProperties p = defaultsEnabled();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OsvComponentLookup lookup = new OsvComponentLookup(p, builder);

        // Zwei PURLs, beide mit identischer GHSA-ID -> nur EIN Detail-Call.
        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andRespond(withSuccess("""
                        {
                          "results": [
                            { "vulns": [ { "id": "GHSA-zzzz-1111" } ] },
                            { "vulns": [ { "id": "GHSA-zzzz-1111" } ] }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://osv.test/v1/vulns/GHSA-zzzz-1111"))
                .andRespond(withSuccess("""
                        { "id": "GHSA-zzzz-1111", "aliases": ["CVE-2030-0001"] }
                        """, MediaType.APPLICATION_JSON));
        // keine weitere Expectation -> MockRestServiceServer haette
        // bei einem zweiten Detail-Call geworfen.

        Map<String, List<String>> r = lookup.findCveIdsForPurls(List.of(
                "pkg:npm/lib-a@1.0",
                "pkg:npm/lib-b@1.0"));

        assertThat(r).hasSize(2);
        assertThat(r.get("pkg:npm/lib-a@1.0")).containsExactly("CVE-2030-0001");
        assertThat(r.get("pkg:npm/lib-b@1.0")).containsExactly("CVE-2030-0001");
        server.verify();
    }

    @Test
    @DisplayName("Retry-After: 429 wird einmalig nach Retry-After-Sekunden wiederholt")
    void retryAfter429WirdEinmaligWiederholt() {
        OsvProperties p = defaultsEnabled();
        p.setMaxRetryAfterSeconds(5);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AtomicInteger slept = new AtomicInteger(-1);
        OsvComponentLookup lookup = new OsvComponentLookup(
                p, builder, seconds -> slept.set(seconds));

        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "2")
                        .body(""));
        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andRespond(withSuccess("""
                        { "results": [ { "vulns": [ { "id": "CVE-2030-0001" } ] } ] }
                        """, MediaType.APPLICATION_JSON));

        Map<String, List<String>> r = lookup.findCveIdsForPurls(List.of(
                "pkg:npm/foo@1.0"));

        assertThat(slept.get()).isEqualTo(2);
        assertThat(r.get("pkg:npm/foo@1.0")).containsExactly("CVE-2030-0001");
        server.verify();
    }

    @Test
    @DisplayName("Retry-After: 429 ohne Header wartet Default 1 s")
    void retryAfter429OhneHeader() {
        OsvProperties p = defaultsEnabled();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AtomicInteger slept = new AtomicInteger(-1);
        OsvComponentLookup lookup = new OsvComponentLookup(
                p, builder, seconds -> slept.set(seconds));

        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body(""));
        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andRespond(withSuccess("""
                        { "results": [ { "vulns": [ { "id": "CVE-2030-0002" } ] } ] }
                        """, MediaType.APPLICATION_JSON));

        Map<String, List<String>> r = lookup.findCveIdsForPurls(List.of(
                "pkg:npm/foo@1.0"));

        assertThat(slept.get()).isEqualTo(1);
        assertThat(r.get("pkg:npm/foo@1.0")).containsExactly("CVE-2030-0002");
    }

    @Test
    @DisplayName("Retry-After: Obergrenze durch maxRetryAfterSeconds greift")
    void retryAfterObergrenze() {
        OsvProperties p = defaultsEnabled();
        p.setMaxRetryAfterSeconds(3);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AtomicInteger slept = new AtomicInteger(-1);
        OsvComponentLookup lookup = new OsvComponentLookup(
                p, builder, seconds -> slept.set(seconds));

        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "999")
                        .body(""));
        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andRespond(withSuccess("""
                        { "results": [ { "vulns": [ { "id": "CVE-2030-0003" } ] } ] }
                        """, MediaType.APPLICATION_JSON));

        lookup.findCveIdsForPurls(List.of("pkg:npm/foo@1.0"));

        assertThat(slept.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Retry-After: retryOn429=false wirft weiter, Map bleibt leer")
    void retryDeaktiviert() {
        OsvProperties p = defaultsEnabled();
        p.setRetryOn429(false);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OsvComponentLookup lookup = new OsvComponentLookup(
                p, builder, seconds -> {});

        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body(""));

        Map<String, List<String>> r = lookup.findCveIdsForPurls(List.of(
                "pkg:npm/foo@1.0"));
        assertThat(r).isEmpty();
    }

    @Test
    @DisplayName("Retry-After: zweimal 429 landet als leere Map")
    void retryZweimal429LeereMap() {
        OsvProperties p = defaultsEnabled();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OsvComponentLookup lookup = new OsvComponentLookup(
                p, builder, seconds -> {});

        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "0").body(""));
        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "0").body(""));

        Map<String, List<String>> r = lookup.findCveIdsForPurls(List.of(
                "pkg:npm/foo@1.0"));

        assertThat(r).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("findCveIdsForPurls: extrahiert CVE-Aliase aus GHSA-Eintraegen")
    void ghsaAliasWirdAlsCveExtrahiert() {
        OsvProperties p = defaultsEnabled();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OsvComponentLookup lookup = new OsvComponentLookup(p, builder);

        server.expect(requestTo("https://osv.test/v1/querybatch"))
                .andRespond(withSuccess("""
                        {
                          "results": [
                            { "vulns": [
                                { "id": "GHSA-abcd-1234",
                                  "aliases": ["CVE-2024-99", "GO-2024-1"] }
                              ] }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        Map<String, List<String>> r = lookup.findCveIdsForPurls(List.of(
                "pkg:npm/foo@1.0"));
        assertThat(r.get("pkg:npm/foo@1.0")).containsExactly("CVE-2024-99");
        server.verify();
    }
}
