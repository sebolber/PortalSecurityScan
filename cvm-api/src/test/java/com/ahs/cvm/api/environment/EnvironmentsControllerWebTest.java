package com.ahs.cvm.api.environment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.environment.EnvironmentQueryService;
import com.ahs.cvm.application.environment.EnvironmentQueryService.EnvironmentKeyAlreadyExistsException;
import com.ahs.cvm.application.environment.EnvironmentView;
import com.ahs.cvm.domain.enums.EnvironmentStage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = EnvironmentsController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class EnvironmentsControllerWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;
    @MockBean EnvironmentQueryService service;

    @Test
    @DisplayName("GET /api/v1/environments: alphabetisch sortierte Liste")
    void list() throws Exception {
        given(service.listAll()).willReturn(List.of(
                new EnvironmentView(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "ABN-TEST", "Abnahme", EnvironmentStage.ABN,
                        "default", null),
                new EnvironmentView(
                        UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        "PROD-TEST", "Produktiv", EnvironmentStage.PROD,
                        "default", null)));

        mockMvc.perform(get("/api/v1/environments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("ABN-TEST"))
                .andExpect(jsonPath("$[1].key").value("PROD-TEST"));
    }

    @Test
    @DisplayName("GET: leer -> []")
    void leer() throws Exception {
        given(service.listAll()).willReturn(List.of());
        mockMvc.perform(get("/api/v1/environments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("POST: gueltiger Request liefert 201 + Location")
    void createOk() throws Exception {
        UUID newId = UUID.randomUUID();
        given(service.create(any()))
                .willReturn(new EnvironmentView(
                        newId, "CI", "Continuous Integration",
                        EnvironmentStage.DEV, "default", null));

        Map<String, Object> body = Map.of(
                "key", "CI",
                "name", "Continuous Integration",
                "stage", "DEV",
                "tenant", "default");

        mockMvc.perform(post("/api/v1/environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/environments/" + newId))
                .andExpect(jsonPath("$.key").value("CI"))
                .andExpect(jsonPath("$.stage").value("DEV"));
    }

    @Test
    @DisplayName("POST: doppelter key -> 409")
    void createDuplicate() throws Exception {
        willThrow(new EnvironmentKeyAlreadyExistsException("CI"))
                .given(service).create(any());

        Map<String, Object> body = Map.of(
                "key", "CI",
                "name", "Continuous Integration",
                "stage", "DEV");

        mockMvc.perform(post("/api/v1/environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("environment_key_exists"));
    }

    @Test
    @DisplayName("POST: fehlender Name -> 400 via Bean Validation")
    void createInvalid() throws Exception {
        Map<String, Object> body = Map.of(
                "key", "CI",
                "stage", "DEV");

        mockMvc.perform(post("/api/v1/environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
