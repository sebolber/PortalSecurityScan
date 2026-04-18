package com.ahs.cvm.ai.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.llm.AiCallAuditService;
import com.ahs.cvm.llm.LlmClient;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClient.TokenUsage;
import com.ahs.cvm.llm.LlmClientSelector;
import com.ahs.cvm.llm.prompt.PromptTemplateLoader;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.product.Product;
import com.ahs.cvm.persistence.product.ProductVersion;
import com.ahs.cvm.persistence.scan.Scan;
import com.ahs.cvm.persistence.scan.ScanRepository;
import com.ahs.cvm.persistence.summary.ScanDeltaSummaryEntity;
import com.ahs.cvm.persistence.summary.ScanDeltaSummaryEntityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScanDeltaSummaryServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final UUID PV_ID = UUID.randomUUID();
    private static final UUID ENV_ID = UUID.randomUUID();

    private ScanRepository scanRepository;
    private ScanDeltaCalculator calculator;
    private AiCallAuditService auditService;
    private LlmClientSelector clientSelector;
    private ScanDeltaSummaryEntityRepository persistRepository;
    private ScanDeltaSummaryService service;

    @BeforeEach
    void setUp() {
        scanRepository = mock(ScanRepository.class);
        calculator = mock(ScanDeltaCalculator.class);
        auditService = mock(AiCallAuditService.class);
        clientSelector = mock(LlmClientSelector.class);
        persistRepository = mock(ScanDeltaSummaryEntityRepository.class);
        LlmClient client = mock(LlmClient.class);
        given(client.modelId()).willReturn("claude-sonnet-4-6");
        given(clientSelector.select(any(), anyString())).willReturn(client);
        service = new ScanDeltaSummaryService(
                scanRepository, calculator, auditService, clientSelector,
                new PromptTemplateLoader(), persistRepository,
                Clock.fixed(Instant.parse("2026-04-18T10:00:00Z"), ZoneOffset.UTC),
                1);
    }

    private Scan scan(UUID id, Instant ts) {
        Product p = Product.builder().id(UUID.randomUUID()).name("PortalCore-Test").build();
        ProductVersion pv = ProductVersion.builder()
                .id(PV_ID).version("1.14.2-test").product(p).build();
        Environment env = Environment.builder()
                .id(ENV_ID).name("REF-TEST").build();
        return Scan.builder()
                .id(id).productVersion(pv).environment(env).scannedAt(ts).build();
    }

    private LlmResponse responseWith(String json) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        return new LlmResponse(node, json, new TokenUsage(40, 30),
                Duration.ofMillis(80), "claude-sonnet-4-6");
    }

    @Test
    @DisplayName("DeltaSummary: Initial-Run liefert statischen Text ohne LLM-Call")
    void initialRun() {
        UUID id = UUID.randomUUID();
        given(scanRepository.findById(id)).willReturn(Optional.of(scan(id, Instant.now())));
        given(scanRepository.findByProductVersionIdOrderByScannedAtDesc(PV_ID))
                .willReturn(List.of(scan(id, Instant.now())));
        given(calculator.calculate(id, null)).willReturn(new ScanDelta(
                List.of("CVE-1", "CVE-2"), List.of(), List.of(), List.of()));

        ScanDeltaSummary s = service.summarize(id);

        assertThat(s.llmAufgerufen()).isFalse();
        assertThat(s.previousScanId()).isNull();
        assertThat(s.shortText()).contains("Initial-Scan");
        verify(auditService, never()).execute(any(), any());
    }

    @Test
    @DisplayName("DeltaSummary: Diff unter Mindestschwelle -> kein LLM-Call")
    void unterMindestSchwelle() {
        UUID curr = UUID.randomUUID();
        UUID prev = UUID.randomUUID();
        given(scanRepository.findById(curr)).willReturn(Optional.of(scan(curr, Instant.now())));
        given(scanRepository.findByProductVersionIdOrderByScannedAtDesc(PV_ID))
                .willReturn(List.of(scan(curr, Instant.now()), scan(prev, Instant.now().minusSeconds(60))));
        given(calculator.calculate(curr, prev)).willReturn(
                new ScanDelta(List.of(), List.of(), List.of(), List.of()));

        ScanDeltaSummary s = service.summarize(curr);

        assertThat(s.llmAufgerufen()).isFalse();
        assertThat(s.previousScanId()).isEqualTo(prev);
        assertThat(s.shortText()).contains("Keine relevanten Aenderungen");
        verify(auditService, never()).execute(any(), any());
    }

    @Test
    @DisplayName("DeltaSummary: ueber Mindestschwelle ruft LLM und liefert short+long")
    void ueberMindestSchwelle() throws Exception {
        UUID curr = UUID.randomUUID();
        UUID prev = UUID.randomUUID();
        given(scanRepository.findById(curr)).willReturn(Optional.of(scan(curr, Instant.now())));
        given(scanRepository.findByProductVersionIdOrderByScannedAtDesc(PV_ID))
                .willReturn(List.of(scan(curr, Instant.now()), scan(prev, Instant.now().minusSeconds(60))));
        given(calculator.calculate(curr, prev)).willReturn(new ScanDelta(
                List.of("CVE-NEU"), List.of(), List.of(), List.of()));
        given(auditService.execute(any(), any())).willReturn(responseWith("""
                {"short":"1 neue CVE.","long":"Im aktuellen Scan ist CVE-NEU neu."}
                """));

        ScanDeltaSummary s = service.summarize(curr);

        assertThat(s.llmAufgerufen()).isTrue();
        assertThat(s.shortText()).isEqualTo("1 neue CVE.");
        assertThat(s.longText()).contains("CVE-NEU");
        verify(persistRepository).save(any(ScanDeltaSummaryEntity.class));
    }

    @Test
    @DisplayName("DeltaSummary: Initial-Run wird auch persistiert")
    void initialRunPersistiert() {
        UUID id = UUID.randomUUID();
        given(scanRepository.findById(id)).willReturn(Optional.of(scan(id, Instant.now())));
        given(scanRepository.findByProductVersionIdOrderByScannedAtDesc(PV_ID))
                .willReturn(List.of(scan(id, Instant.now())));
        given(calculator.calculate(id, null)).willReturn(new ScanDelta(
                List.of("CVE-1"), List.of(), List.of(), List.of()));

        service.summarize(id);

        verify(persistRepository).save(any(ScanDeltaSummaryEntity.class));
    }

    @Test
    @DisplayName("DeltaSummary: unbekannter Scan wirft IllegalArgumentException")
    void unbekannt() {
        UUID id = UUID.randomUUID();
        given(scanRepository.findById(id)).willReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.summarize(id))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
