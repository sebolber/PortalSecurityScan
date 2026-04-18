package com.ahs.cvm.api.waiver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.waiver.WaiverNotApplicableException;
import com.ahs.cvm.application.waiver.WaiverService;
import com.ahs.cvm.application.waiver.WaiverService.GrantCommand;
import com.ahs.cvm.application.waiver.WaiverView;
import com.ahs.cvm.domain.enums.WaiverStatus;
import java.time.Instant;
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
        controllers = WaiverController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(WaiverExceptionHandler.class)
class WaiverControllerWebTest {

    private static final UUID WAIVER_ID = UUID.fromString(
            "11111111-1111-1111-1111-111111111111");
    private static final UUID ASSESSMENT_ID = UUID.fromString(
            "22222222-2222-2222-2222-222222222222");
    private static final Instant VALID_UNTIL = Instant.parse("2027-01-01T00:00:00Z");

    @Autowired MockMvc mockMvc;
    @MockBean WaiverService service;

    private WaiverView view(WaiverStatus status) {
        return new WaiverView(
                WAIVER_ID, ASSESSMENT_ID, "Grund", "a.admin@ahs.test",
                VALID_UNTIL, 90, status,
                Instant.parse("2026-04-18T10:00:00Z"), null, null);
    }

    @Test
    @DisplayName("POST /waivers: 201 mit Waiver-View")
    void grant() throws Exception {
        given(service.grant(any(GrantCommand.class)))
                .willReturn(view(WaiverStatus.ACTIVE));
        mockMvc.perform(post("/api/v1/waivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assessmentId":"%s",
                                 "reason":"Restrisiko akzeptiert",
                                 "grantedBy":"a.admin@ahs.test",
                                 "validUntil":"2027-01-01T00:00:00Z",
                                 "reviewIntervalDays":90}
                                """.formatted(ASSESSMENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(WAIVER_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /waivers: Vier-Augen-Verstoss -> 409")
    void vierAugen() throws Exception {
        willThrow(new WaiverService.VierAugenViolationException(
                "Vier-Augen-Verstoss: grantedBy == decidedBy (x)"))
                .given(service).grant(any());
        mockMvc.perform(post("/api/v1/waivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assessmentId":"%s",
                                 "reason":"r","grantedBy":"x",
                                 "validUntil":"2027-01-01T00:00:00Z"}
                                """.formatted(ASSESSMENT_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("vier_augen_violation"));
    }

    @Test
    @DisplayName("POST /waivers: nicht-passende Strategie -> 422")
    void nichtPassend() throws Exception {
        willThrow(new WaiverNotApplicableException(
                "Waiver nur fuer ACCEPT_RISK oder WORKAROUND"))
                .given(service).grant(any());
        mockMvc.perform(post("/api/v1/waivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assessmentId":"%s",
                                 "reason":"r","grantedBy":"a.admin@ahs.test",
                                 "validUntil":"2027-01-01T00:00:00Z"}
                                """.formatted(ASSESSMENT_ID)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("waiver_not_applicable"));
    }

    @Test
    @DisplayName("POST /waivers: leerer Reason -> 400")
    void reasonLeer() throws Exception {
        mockMvc.perform(post("/api/v1/waivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assessmentId":"%s",
                                 "reason":"","grantedBy":"a.admin@ahs.test",
                                 "validUntil":"2027-01-01T00:00:00Z"}
                                """.formatted(ASSESSMENT_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /waivers/{id}/extend: 200, neues validUntil")
    void extend() throws Exception {
        given(service.extend(eq(WAIVER_ID), any(), eq("j.meyer@ahs.test")))
                .willReturn(view(WaiverStatus.ACTIVE));
        mockMvc.perform(post("/api/v1/waivers/" + WAIVER_ID + "/extend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"validUntil":"2027-06-01T00:00:00Z",
                                 "extendedBy":"j.meyer@ahs.test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(WAIVER_ID.toString()));
    }

    @Test
    @DisplayName("POST /waivers/{id}/revoke: 200, Status REVOKED")
    void revoke() throws Exception {
        given(service.revoke(eq(WAIVER_ID), eq("a.admin@ahs.test"), eq("ueberholt")))
                .willReturn(view(WaiverStatus.REVOKED));
        mockMvc.perform(post("/api/v1/waivers/" + WAIVER_ID + "/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"revokedBy":"a.admin@ahs.test","reason":"ueberholt"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }

    @Test
    @DisplayName("GET /waivers?status=EXPIRING_SOON: liefert Liste")
    void liste() throws Exception {
        given(service.byStatus(WaiverStatus.EXPIRING_SOON))
                .willReturn(List.of(view(WaiverStatus.EXPIRING_SOON)));
        mockMvc.perform(get("/api/v1/waivers").param("status", "EXPIRING_SOON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("EXPIRING_SOON"));
    }
}
