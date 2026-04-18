package com.ahs.cvm.api.environment;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.environment.EnvironmentQueryService;
import com.ahs.cvm.application.environment.EnvironmentView;
import com.ahs.cvm.domain.enums.EnvironmentStage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = EnvironmentsController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class EnvironmentsControllerWebTest {

    @Autowired MockMvc mockMvc;
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
}
