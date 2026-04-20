package com.ahs.cvm.api.reachability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.ai.reachability.ReachabilityAgent;
import com.ahs.cvm.ai.reachability.ReachabilityResult;
import com.ahs.cvm.ai.reachability.ReachabilityResult.CallSite;
import com.ahs.cvm.application.reachability.ReachabilityQueryService;
import com.ahs.cvm.application.reachability.ReachabilitySuggestionView;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = ReachabilityController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(ReachabilityExceptionHandler.class)
class ReachabilityControllerWebTest {

    private static final UUID FINDING = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired MockMvc mockMvc;
    @MockBean ReachabilityAgent agent;
    // Satisfies ReachabilityQueryController constructor (same @ComponentScan).
    @MockBean com.ahs.cvm.application.reachability.ReachabilityQueryService reachabilityQueryService;

    private static final String BODY = """
            {
              "repoUrl":"ssh://git@example/cvm.git",
              "branch":"main",
              "commitSha":"abc1234",
              "vulnerableSymbol":"X.y(Type)",
              "language":"java",
              "instruction":"",
              "triggeredBy":"t.tester@ahs.test"
            }""";

    @Test
    @DisplayName("Reachability-Controller: 200 mit Result-JSON")
    void analyze() throws Exception {
        given(agent.analyze(any())).willReturn(new ReachabilityResult(
                FINDING, UUID.randomUUID(), "ACCEPT", "ok",
                List.of(new CallSite("a.java", 1, "X.y", "STATIC_CONFIG", "n")),
                true, null));

        mockMvc.perform(post("/api/v1/findings/" + FINDING + "/reachability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation").value("ACCEPT"))
                .andExpect(jsonPath("$.callSites[0].file").value("a.java"));
    }

    @Test
    @DisplayName("Reachability-Controller: 404 bei unbekanntem Finding")
    void unbekanntesFinding() throws Exception {
        willThrow(new IllegalArgumentException("Finding nicht gefunden: " + FINDING))
                .given(agent).analyze(any());

        mockMvc.perform(post("/api/v1/findings/" + FINDING + "/reachability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("finding_not_found"));
    }

    @Test
    @DisplayName("Reachability-Controller: 400 bei leerer repoUrl")
    void leereRepoUrl() throws Exception {
        String body = """
                {"repoUrl":"","vulnerableSymbol":"X.y","triggeredBy":"t@x"}""";
        mockMvc.perform(post("/api/v1/findings/" + FINDING + "/reachability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Reachability-Controller: 400 bei fehlendem commitSha")
    void fehlendesCommitSha() throws Exception {
        String body = """
                {"repoUrl":"ssh://git@example/cvm.git",
                 "commitSha":"",
                 "vulnerableSymbol":"X.y",
                 "triggeredBy":"t@x"}""";
        mockMvc.perform(post("/api/v1/findings/" + FINDING + "/reachability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /{id}/reachability/suggestion: 200 mit abgeleitetem Symbol")
    void suggestionHappyPath() throws Exception {
        given(reachabilityQueryService.suggestionForFinding(eq(FINDING)))
                .willReturn(new ReachabilitySuggestionView(
                        FINDING,
                        "pkg:maven/org.apache.commons/commons-text@1.9",
                        "org.apache.commons.text",
                        "java",
                        "Paket-Prefix aus Maven-Koordinaten."));

        mockMvc.perform(get("/api/v1/findings/" + FINDING + "/reachability/suggestion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("org.apache.commons.text"))
                .andExpect(jsonPath("$.language").value("java"))
                .andExpect(jsonPath("$.sourcePurl").value("pkg:maven/org.apache.commons/commons-text@1.9"));
    }

    @Test
    @DisplayName("GET /{id}/reachability/suggestion: 404 bei unbekanntem Finding")
    void suggestionNotFound() throws Exception {
        willThrow(new ReachabilityQueryService.FindingNotFoundException(FINDING))
                .given(reachabilityQueryService).suggestionForFinding(eq(FINDING));

        mockMvc.perform(get("/api/v1/findings/" + FINDING + "/reachability/suggestion"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("finding_not_found"));
    }
}
