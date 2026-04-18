package com.ahs.cvm.ai.executive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.ai.executive.ExecutiveReportService.Audience;
import com.ahs.cvm.application.report.HardeningReportData;
import com.ahs.cvm.application.report.HardeningReportData.Anhang;
import com.ahs.cvm.application.report.HardeningReportData.CveZeile;
import com.ahs.cvm.application.report.HardeningReportData.KennzahlZeile;
import com.ahs.cvm.application.report.HardeningReportData.Kopf;
import com.ahs.cvm.application.report.HardeningReportData.OffenerPunkt;
import com.ahs.cvm.application.report.HardeningReportDataLoader;
import com.ahs.cvm.application.report.HardeningReportPdfRenderer;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.llm.AiCallAuditService;
import com.ahs.cvm.llm.LlmClient;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClient.TokenUsage;
import com.ahs.cvm.llm.LlmClientSelector;
import com.ahs.cvm.llm.prompt.PromptTemplateLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExecutiveReportServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final UUID PV = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID ENV = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    private HardeningReportDataLoader loader;
    private AiCallAuditService auditService;
    private LlmClientSelector selector;
    private ExecutiveReportService service;

    @BeforeEach
    void setUp() {
        loader = mock(HardeningReportDataLoader.class);
        auditService = mock(AiCallAuditService.class);
        selector = mock(LlmClientSelector.class);
        LlmClient client = mock(LlmClient.class);
        given(client.modelId()).willReturn("claude-sonnet-4-6");
        given(selector.select(any(), anyString())).willReturn(client);
        given(loader.load(any())).willReturn(fakeData());
        service = new ExecutiveReportService(
                loader, new HardeningReportPdfRenderer(), auditService, selector,
                new PromptTemplateLoader(),
                Clock.fixed(Instant.parse("2026-04-18T10:00:00Z"), ZoneOffset.UTC));
    }

    private HardeningReportData fakeData() {
        return new HardeningReportData(
                new Kopf("PortalCore-Test", "1.14.2-test", "abc",
                        "REF-TEST", "REFERENCE", "2026-04-18",
                        "a.admin@ahs.test", "v1", Instant.now()),
                AhsSeverity.MEDIUM, "",
                List.of(new KennzahlZeile("Plattform", 1, 2, 3, 0, 0)),
                List.of(new CveZeile("Plattform", "CVE-X",
                        "https://nvd", "7.5", AhsSeverity.HIGH,
                        "Update", "hinweis")),
                List.of(new OffenerPunkt("CVE-Y", AhsSeverity.CRITICAL, "PROPOSED", "offen")),
                new Anhang("", List.of(), Map.of("vex", "")));
    }

    private LlmResponse res(String json) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        return new LlmResponse(node, json, new TokenUsage(50, 30),
                Duration.ofMillis(80), "claude-sonnet-4-6");
    }

    @Test
    @DisplayName("Executive-Report Board: PDF enthaelt Headline und Ampel, max 5 Bullets")
    void board() throws Exception {
        given(auditService.execute(any(), any())).willReturn(res("""
                {"headline":"Weiterbetrieb moeglich mit Auflagen",
                 "ampel":"YELLOW",
                 "bullets":["Bullet 1","Bullet 2","Bullet 3"]}"""));

        var r = service.generate(PV, ENV, Audience.BOARD, "a.admin@ahs.test");

        assertThat(r.summary().ampel()).isEqualTo("YELLOW");
        assertThat(r.summary().bullets()).hasSize(3);
        assertThat(new String(r.pdf(), 0, 5)).startsWith("%PDF");
    }

    @Test
    @DisplayName("Executive-Report: mehr als 5 Bullets werden auf 5 gekappt")
    void maxFiveBullets() throws Exception {
        given(auditService.execute(any(), any())).willReturn(res("""
                {"headline":"X","ampel":"GREEN",
                 "bullets":["A","B","C","D","E","F","G"]}"""));
        var r = service.generate(PV, ENV, Audience.BOARD, "u@x");
        assertThat(r.summary().bullets()).hasSize(5);
    }

    @Test
    @DisplayName("Executive-Report: Bullet > 140 Zeichen wird gekappt")
    void truncateLongBullet() throws Exception {
        String lang = "x".repeat(200);
        given(auditService.execute(any(), any())).willReturn(res(
                "{\"headline\":\"X\",\"ampel\":\"GREEN\",\"bullets\":[\""
                        + lang + "\"]}"));
        var r = service.generate(PV, ENV, Audience.BOARD, "u@x");
        assertThat(r.summary().bullets().get(0).length()).isEqualTo(140);
    }

    @Test
    @DisplayName("Executive-Report: Headline > 80 Zeichen wird gekappt")
    void truncateHeadline() throws Exception {
        String lang = "a".repeat(100);
        given(auditService.execute(any(), any())).willReturn(res(
                "{\"headline\":\"" + lang + "\",\"ampel\":\"GREEN\",\"bullets\":[]}"));
        var r = service.generate(PV, ENV, Audience.BOARD, "u@x");
        assertThat(r.summary().headline().length()).isEqualTo(80);
    }

    @Test
    @DisplayName("Executive-Report Audit: unterscheidet sich vom Board-PDF (anderes Template)")
    void audienceUnterschied() throws Exception {
        given(auditService.execute(any(), any())).willReturn(res("""
                {"headline":"X","ampel":"YELLOW","bullets":["A"]}"""));
        var board = service.generate(PV, ENV, Audience.BOARD, "u@x");
        var audit = service.generate(PV, ENV, Audience.AUDIT, "u@x");

        assertThat(board.pdf()).isNotEqualTo(audit.pdf());
    }

    @Test
    @DisplayName("Executive-Report: ungueltige Ampel -> Fallback YELLOW")
    void ungueltigeAmpel() throws Exception {
        given(auditService.execute(any(), any())).willReturn(res("""
                {"headline":"X","ampel":"OFFLINE","bullets":["A"]}"""));
        var r = service.generate(PV, ENV, Audience.BOARD, "u@x");
        assertThat(r.summary().ampel()).isEqualTo("YELLOW");
    }
}
