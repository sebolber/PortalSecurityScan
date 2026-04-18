package com.ahs.cvm.application.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.application.pipeline.PipelineGateService.GateDecision;
import com.ahs.cvm.application.pipeline.PipelineGateService.GateRequest;
import com.ahs.cvm.application.pipeline.PipelineGateService.GateResult;
import com.ahs.cvm.application.scan.CycloneDxBom;
import com.ahs.cvm.application.scan.CycloneDxParser;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.cve.CveRepository;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.product.ProductVersion;
import com.ahs.cvm.persistence.scan.Scan;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class PipelineGateServiceTest {

    private static final UUID PV = UUID.randomUUID();
    private static final UUID ENV = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-04-18T10:00:00Z");

    private CycloneDxParser parser;
    private CveRepository cveRepo;
    private FindingRepository findingRepo;
    private PipelineGateRateLimiter rateLimiter;
    private ApplicationEventPublisher events;
    private PipelineGateService service;

    @BeforeEach
    void setUp() {
        parser = mock(CycloneDxParser.class);
        cveRepo = mock(CveRepository.class);
        findingRepo = mock(FindingRepository.class);
        rateLimiter = mock(PipelineGateRateLimiter.class);
        events = mock(ApplicationEventPublisher.class);
        given(rateLimiter.tryAcquire(any())).willReturn(true);
        service = new PipelineGateService(parser, cveRepo, findingRepo,
                rateLimiter, events, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private CycloneDxBom bomMit(CycloneDxBom.Vulnerability... vs) {
        return new CycloneDxBom("CycloneDX", "1.6", null, 1, List.of(),
                List.of(vs));
    }

    private CycloneDxBom.Vulnerability vuln(String id, String severity) {
        return new CycloneDxBom.Vulnerability(
                null, id, null,
                severity == null ? List.of()
                        : List.of(new CycloneDxBom.Rating(
                                "CVSSv31", null, null, severity)),
                null, List.of());
    }

    private Cve cve(String key, BigDecimal cvss) {
        return Cve.builder()
                .id(UUID.randomUUID())
                .cveId(key)
                .cvssBaseScore(cvss)
                .build();
    }

    @Test
    @DisplayName("Gate: kein neuer CVE -> PASS")
    void pass() {
        given(parser.parse(any(byte[].class))).willReturn(bomMit());
        GateResult r = service.evaluate(new GateRequest(
                PV, ENV, "main", "MR-1", "{}".getBytes()));
        assertThat(r.gate()).isEqualTo(GateDecision.PASS);
    }

    @Test
    @DisplayName("Gate: neuer HIGH ohne neuen CRITICAL -> WARN")
    void warn() {
        Cve high = cve("CVE-2025-001", new BigDecimal("7.5"));
        given(parser.parse(any(byte[].class))).willReturn(
                bomMit(vuln("CVE-2025-001", "HIGH")));
        given(cveRepo.findByCveId("CVE-2025-001")).willReturn(Optional.of(high));
        given(findingRepo.findByCveId(high.getId())).willReturn(List.of());

        GateResult r = service.evaluate(new GateRequest(
                PV, ENV, "feature/x", "MR-2", "{}".getBytes()));
        assertThat(r.gate()).isEqualTo(GateDecision.WARN);
        assertThat(r.newHigh()).isEqualTo(1);
        assertThat(r.newCritical()).isZero();
    }

    @Test
    @DisplayName("Gate: neuer CRITICAL -> FAIL")
    void fail() {
        Cve crit = cve("CVE-2025-002", new BigDecimal("9.5"));
        given(parser.parse(any(byte[].class))).willReturn(
                bomMit(vuln("CVE-2025-002", "CRITICAL")));
        given(cveRepo.findByCveId("CVE-2025-002")).willReturn(Optional.of(crit));
        given(findingRepo.findByCveId(crit.getId())).willReturn(List.of());

        GateResult r = service.evaluate(new GateRequest(
                PV, ENV, "main", "MR-3", "{}".getBytes()));
        assertThat(r.gate()).isEqualTo(GateDecision.FAIL);
        assertThat(r.newCritical()).isEqualTo(1);
    }

    @Test
    @DisplayName("Gate: bereits bekannter CVE zaehlt NICHT als neu")
    void bekannt() {
        Cve crit = cve("CVE-2025-003", new BigDecimal("9.5"));
        given(parser.parse(any(byte[].class))).willReturn(
                bomMit(vuln("CVE-2025-003", "CRITICAL")));
        given(cveRepo.findByCveId("CVE-2025-003")).willReturn(Optional.of(crit));

        ProductVersion pv = ProductVersion.builder().id(PV).build();
        Scan scan = Scan.builder().id(UUID.randomUUID()).productVersion(pv).build();
        Finding existing = Finding.builder().id(UUID.randomUUID())
                .scan(scan).cve(crit).build();
        given(findingRepo.findByCveId(crit.getId())).willReturn(List.of(existing));

        GateResult r = service.evaluate(new GateRequest(
                PV, ENV, "main", "MR-4", "{}".getBytes()));
        assertThat(r.gate()).isEqualTo(GateDecision.PASS);
    }

    @Test
    @DisplayName("Gate: unbekannter CVE in Katalog -> INFORMATIONAL Detail, kein FAIL")
    void unbekannter() {
        given(parser.parse(any(byte[].class))).willReturn(
                bomMit(vuln("CVE-9999-UNKNOWN", null)));
        given(cveRepo.findByCveId("CVE-9999-UNKNOWN")).willReturn(Optional.empty());

        GateResult r = service.evaluate(new GateRequest(
                PV, ENV, "main", "MR-5", "{}".getBytes()));
        assertThat(r.gate()).isEqualTo(GateDecision.PASS);
        assertThat(r.details()).anyMatch(
                d -> "CVE-9999-UNKNOWN".equals(d.cveKey())
                && d.severity() == AhsSeverity.INFORMATIONAL);
    }

    @Test
    @DisplayName("Gate: leere SBOM -> IllegalArgumentException")
    void leereSbom() {
        assertThatThrownBy(() -> service.evaluate(new GateRequest(
                PV, ENV, "m", "M", new byte[0])))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Gate: Rate-Limit greift -> GateRateLimitException, kein Parse")
    void rateLimit() {
        given(rateLimiter.tryAcquire(PV.toString())).willReturn(false);
        assertThatThrownBy(() -> service.evaluate(new GateRequest(
                PV, ENV, "m", "M", "{}".getBytes())))
                .isInstanceOf(PipelineGateService.GateRateLimitException.class);
        verify(events, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Gate: publiziert PipelineGateEvaluatedEvent mit repoUrl und MR-Id")
    void publiziertEvent() {
        given(parser.parse(any(byte[].class))).willReturn(bomMit());
        GateResult r = service.evaluate(new GateRequest(
                PV, ENV, "main", "42", "{}".getBytes(),
                "https://github.com/adesso-health/portal-core"));
        assertThat(r.gate()).isEqualTo(GateDecision.PASS);
        verify(events).publishEvent(new PipelineGateEvaluatedEvent(
                PV, "https://github.com/adesso-health/portal-core", "42", r));
    }
}
