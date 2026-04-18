package com.ahs.cvm.integration.osv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
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
