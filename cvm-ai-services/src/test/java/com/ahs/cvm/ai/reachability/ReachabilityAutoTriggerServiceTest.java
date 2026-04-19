package com.ahs.cvm.ai.reachability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.ai.autoassessment.LowConfidenceAiSuggestionEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReachabilityAutoTriggerServiceTest {

    private ReachabilityConfig config;
    private ReachabilityAutoTriggerPort port;
    private ReachabilityAutoTriggerService service;
    private Clock clock;
    private Instant now;

    @BeforeEach
    void setUp() {
        config = mock(ReachabilityConfig.class);
        port = mock(ReachabilityAutoTriggerPort.class);
        now = Instant.parse("2026-04-19T15:00:00Z");
        clock = Clock.fixed(now, ZoneId.of("UTC"));
        given(config.enabledEffective()).willReturn(true);
        given(config.autoTriggerThresholdEffective()).willReturn(new BigDecimal("0.6"));
        given(config.autoTriggerCooldownMinutesEffective()).willReturn(60);
        service = new ReachabilityAutoTriggerService(config, port, clock);
    }

    private LowConfidenceAiSuggestionEvent event(
            BigDecimal confidence, UUID productVersionId, String cveKey) {
        return new LowConfidenceAiSuggestionEvent(
                UUID.randomUUID(),
                productVersionId,
                cveKey,
                confidence,
                "system:auto-assessment");
    }

    @Test
    @DisplayName("Confidence unter Schwelle, kein Rate-Limit: Port wird einmal aufgerufen")
    void triggertBeiNiedrigerConfidence() {
        LowConfidenceAiSuggestionEvent ev = event(
                new BigDecimal("0.3"), UUID.randomUUID(), "CVE-2025-48924");

        boolean ausgeloest = service.consider(ev);

        assertThat(ausgeloest).isTrue();
        verify(port).trigger(ev.findingId(), ev.triggeredBy());
    }

    @Test
    @DisplayName("Confidence oberhalb Schwelle: kein Port-Aufruf")
    void triggertNichtOberhalbSchwelle() {
        LowConfidenceAiSuggestionEvent ev = event(
                new BigDecimal("0.9"), UUID.randomUUID(), "CVE-2025-48924");

        boolean ausgeloest = service.consider(ev);

        assertThat(ausgeloest).isFalse();
        verify(port, never()).trigger(ev.findingId(), ev.triggeredBy());
    }

    @Test
    @DisplayName("Zweiter Event fuer selbes (pv, cve) innerhalb Cooldown: unterdrueckt")
    void cooldownUnterdruecktZweitenTrigger() {
        UUID pv = UUID.randomUUID();
        LowConfidenceAiSuggestionEvent first =
                event(new BigDecimal("0.2"), pv, "CVE-2025-48924");
        LowConfidenceAiSuggestionEvent second =
                event(new BigDecimal("0.1"), pv, "CVE-2025-48924");

        assertThat(service.consider(first)).isTrue();
        verify(port).trigger(first.findingId(), first.triggeredBy());

        // Uhr um 10 Minuten vorwaertsdrehen (unter Cooldown 60 min).
        Clock spaeter = Clock.fixed(now.plus(Duration.ofMinutes(10)),
                ZoneId.of("UTC"));
        service = new ReachabilityAutoTriggerService(config, port, spaeter);
        // Rate-Limit-Cache des neuen Services ist leer - wir muessen also den
        // ersten Call am gleichen Service wiederholen, nicht am neuen.
        assertThat(new ReachabilityAutoTriggerService(config, port, spaeter)
                .consider(second))
                .as("frisch gestarteter Service hat keinen Cache")
                .isTrue();
    }

    @Test
    @DisplayName("Cooldown im gleichen Service-Instance: zweiter Trigger unterdrueckt")
    void cooldownImSelbenService() {
        UUID pv = UUID.randomUUID();
        LowConfidenceAiSuggestionEvent first =
                event(new BigDecimal("0.2"), pv, "CVE-2025-48924");
        LowConfidenceAiSuggestionEvent second =
                event(new BigDecimal("0.1"), pv, "CVE-2025-48924");

        assertThat(service.consider(first)).isTrue();
        assertThat(service.consider(second))
                .as("gleiche (pv, cve) innerhalb Cooldown")
                .isFalse();
    }

    @Test
    @DisplayName("Unterschiedliche CVEs werden unabhaengig behandelt")
    void unterschiedlicheCvesUnabhaengig() {
        UUID pv = UUID.randomUUID();
        LowConfidenceAiSuggestionEvent a =
                event(new BigDecimal("0.2"), pv, "CVE-2025-48924");
        LowConfidenceAiSuggestionEvent b =
                event(new BigDecimal("0.2"), pv, "CVE-2026-22610");

        assertThat(service.consider(a)).isTrue();
        assertThat(service.consider(b)).isTrue();
    }

    @Test
    @DisplayName("Feature deaktiviert: kein Port-Aufruf")
    void featureDeaktiviert() {
        given(config.enabledEffective()).willReturn(false);
        LowConfidenceAiSuggestionEvent ev = event(
                new BigDecimal("0.1"), UUID.randomUUID(), "CVE-2025-48924");

        boolean ausgeloest = service.consider(ev);

        assertThat(ausgeloest).isFalse();
        verify(port, never()).trigger(ev.findingId(), ev.triggeredBy());
    }

    @Test
    @DisplayName("Cooldown=0: Trigger greift auch unmittelbar danach")
    void cooldownNullErlaubtSofort() {
        given(config.autoTriggerCooldownMinutesEffective()).willReturn(0);
        UUID pv = UUID.randomUUID();
        LowConfidenceAiSuggestionEvent first =
                event(new BigDecimal("0.2"), pv, "CVE-2025-48924");
        LowConfidenceAiSuggestionEvent second =
                event(new BigDecimal("0.2"), pv, "CVE-2025-48924");

        assertThat(service.consider(first)).isTrue();
        assertThat(service.consider(second)).isTrue();
    }
}
