package com.ahs.cvm.application.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AlertSeverity;
import com.ahs.cvm.domain.enums.AlertTriggerArt;
import com.ahs.cvm.persistence.alert.AlertEvent;
import com.ahs.cvm.persistence.alert.AlertEventRepository;
import com.ahs.cvm.persistence.alert.AlertRule;
import com.ahs.cvm.persistence.alert.AlertRuleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AlertEvaluatorTest {

    private AlertRuleRepository ruleRepository;
    private AlertEventRepository eventRepository;
    private AlertDispatcher dispatcher;
    private Clock clock;
    private AlertEvaluator evaluator;

    private final Instant t0 = Instant.parse("2026-04-18T10:00:00Z");
    private final UUID ruleId = UUID.randomUUID();
    private AlertRule rule;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(AlertRuleRepository.class);
        eventRepository = mock(AlertEventRepository.class);
        dispatcher = mock(AlertDispatcher.class);
        clock = Clock.fixed(t0, ZoneOffset.UTC);
        evaluator = new AlertEvaluator(ruleRepository, eventRepository, dispatcher, clock);

        rule = AlertRule.builder()
                .name("KEV-Hit")
                .triggerArt(AlertTriggerArt.KEV_HIT)
                .severity(AlertSeverity.CRITICAL)
                .cooldownMinutes(60)
                .templateName("alert-kev-hit")
                .recipients(List.of("alerts@ahs.test"))
                .enabled(Boolean.TRUE)
                .build();
        rule.setId(ruleId);
    }

    private AlertContext context(String key, Instant when) {
        return new AlertContext(
                AlertTriggerArt.KEV_HIT, key, AhsSeverity.CRITICAL,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "summary", when, Map.of("cveKey", "CVE-2025-1"));
    }

    @Test
    @DisplayName("Erstes Feuern erzeugt AlertEvent und ruft Dispatcher")
    void erstesFeuern_loestAusUndSpeichertEvent() {
        given(ruleRepository.findByEnabledTrueAndTriggerArt(AlertTriggerArt.KEV_HIT))
                .willReturn(List.of(rule));
        given(eventRepository.findByRuleIdAndTriggerKey(eq(ruleId), any()))
                .willReturn(Optional.empty());

        var outcome = evaluator.evaluate(context("CVE-2025-1|prod", t0));

        assertThat(outcome.gefeuert()).isEqualTo(1);
        assertThat(outcome.unterdrueckt()).isEqualTo(0);
        verify(dispatcher).dispatch(eq(rule), any(AlertContext.class));
        ArgumentCaptor<AlertEvent> ev = ArgumentCaptor.forClass(AlertEvent.class);
        verify(eventRepository).save(ev.capture());
        assertThat(ev.getValue().getLastFiredAt()).isEqualTo(t0);
        assertThat(ev.getValue().getSuppressedCount()).isZero();
    }

    @Test
    @DisplayName("Cooldown: zweiter Trigger innerhalb 60 Minuten unterdrueckt, suppressedCount steigt")
    void cooldownUnterdruecktInnerhalbDerSchwelle() {
        AlertEvent vorhanden = AlertEvent.builder()
                .ruleId(ruleId)
                .triggerKey("CVE-2025-1|prod")
                .lastFiredAt(t0.minusSeconds(30 * 60))
                .suppressedCount(0)
                .build();
        given(ruleRepository.findByEnabledTrueAndTriggerArt(AlertTriggerArt.KEV_HIT))
                .willReturn(List.of(rule));
        given(eventRepository.findByRuleIdAndTriggerKey(eq(ruleId), eq("CVE-2025-1|prod")))
                .willReturn(Optional.of(vorhanden));

        var outcome = evaluator.evaluate(context("CVE-2025-1|prod", t0));

        assertThat(outcome.gefeuert()).isZero();
        assertThat(outcome.unterdrueckt()).isEqualTo(1);
        verify(dispatcher, never()).dispatch(any(), any());
        ArgumentCaptor<AlertEvent> ev = ArgumentCaptor.forClass(AlertEvent.class);
        verify(eventRepository).save(ev.capture());
        assertThat(ev.getValue().getSuppressedCount()).isEqualTo(1);
        assertThat(ev.getValue().getLastFiredAt()).isEqualTo(t0.minusSeconds(30 * 60));
    }

    @Test
    @DisplayName("Cooldown abgelaufen: feuert wieder und setzt suppressedCount zurueck")
    void cooldownAbgelaufenFeuert() {
        AlertEvent vorhanden = AlertEvent.builder()
                .ruleId(ruleId)
                .triggerKey("CVE-2025-1|prod")
                .lastFiredAt(t0.minusSeconds(61 * 60))
                .suppressedCount(3)
                .build();
        given(ruleRepository.findByEnabledTrueAndTriggerArt(AlertTriggerArt.KEV_HIT))
                .willReturn(List.of(rule));
        given(eventRepository.findByRuleIdAndTriggerKey(eq(ruleId), eq("CVE-2025-1|prod")))
                .willReturn(Optional.of(vorhanden));

        var outcome = evaluator.evaluate(context("CVE-2025-1|prod", t0));

        assertThat(outcome.gefeuert()).isEqualTo(1);
        verify(dispatcher, times(1)).dispatch(any(), any());
        ArgumentCaptor<AlertEvent> ev = ArgumentCaptor.forClass(AlertEvent.class);
        verify(eventRepository).save(ev.capture());
        assertThat(ev.getValue().getSuppressedCount()).isZero();
        assertThat(ev.getValue().getLastFiredAt()).isEqualTo(t0);
    }

    @Test
    @DisplayName("Keine aktiven Regeln liefert leeres Ergebnis ohne Dispatcher-Aufruf")
    void keineRegeln_keinDispatcher() {
        given(ruleRepository.findByEnabledTrueAndTriggerArt(AlertTriggerArt.KEV_HIT))
                .willReturn(List.of());

        var outcome = evaluator.evaluate(context("CVE-2025-1|prod", t0));

        assertThat(outcome.gefeuert()).isZero();
        verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    @DisplayName("Dispatcher-Fehler bricht Lauf nicht ab und last_fired wird nicht gesetzt")
    void dispatcherFehlerKeinFeuern() {
        given(ruleRepository.findByEnabledTrueAndTriggerArt(AlertTriggerArt.KEV_HIT))
                .willReturn(List.of(rule));
        given(eventRepository.findByRuleIdAndTriggerKey(eq(ruleId), any()))
                .willReturn(Optional.empty());
        org.mockito.Mockito.doThrow(new MailDispatchException("offline", null))
                .when(dispatcher).dispatch(any(), any());

        var outcome = evaluator.evaluate(context("CVE-2025-1|prod", t0));

        assertThat(outcome.gefeuert()).isZero();
        verify(eventRepository, never()).save(any());
    }
}
