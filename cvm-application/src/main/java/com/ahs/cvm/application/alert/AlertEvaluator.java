package com.ahs.cvm.application.alert;

import com.ahs.cvm.persistence.alert.AlertEvent;
import com.ahs.cvm.persistence.alert.AlertEventRepository;
import com.ahs.cvm.persistence.alert.AlertRule;
import com.ahs.cvm.persistence.alert.AlertRuleRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wertet einen {@link AlertContext} gegen alle aktiven Regeln aus.
 *
 * <p>Auswertung:
 * <ol>
 *   <li>Alle aktiven Regeln zur passenden {@code triggerArt} laden.</li>
 *   <li>Pro Regel pruefen, ob der Cooldown abgelaufen ist (oder es noch
 *       nie ein Event gab).</li>
 *   <li>Bei Cooldown-Treffer: Dispatcher aufrufen, danach
 *       {@code last_fired_at} aktualisieren.</li>
 *   <li>Bei Cooldown-Verletzung: Suppressed-Counter erhoehen, kein
 *       Versand.</li>
 * </ol>
 */
@Service
public class AlertEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluator.class);

    private final AlertRuleRepository ruleRepository;
    private final AlertEventRepository eventRepository;
    private final AlertDispatcher dispatcher;
    private final Clock clock;

    public AlertEvaluator(
            AlertRuleRepository ruleRepository,
            AlertEventRepository eventRepository,
            AlertDispatcher dispatcher,
            Clock clock) {
        this.ruleRepository = ruleRepository;
        this.eventRepository = eventRepository;
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    @Transactional
    public AlertOutcome evaluate(AlertContext context) {
        List<AlertRule> regeln = ruleRepository
                .findByEnabledTrueAndTriggerArt(context.triggerArt());
        if (regeln.isEmpty()) {
            log.debug("Keine aktiven Regeln fuer triggerArt={}", context.triggerArt());
            return new AlertOutcome(0, 0);
        }
        Instant jetzt = Instant.now(clock);
        int gefeuert = 0;
        int unterdrueckt = 0;
        for (AlertRule rule : regeln) {
            if (sollUnterdruecken(rule, context.triggerKey(), jetzt)) {
                inkrementiereSuppressed(rule.getId(), context.triggerKey(), jetzt);
                unterdrueckt++;
                continue;
            }
            try {
                dispatcher.dispatch(rule, context);
                merkeFeuern(rule.getId(), context.triggerKey(), jetzt);
                gefeuert++;
            } catch (MailDispatchException ex) {
                log.warn("Alert-Dispatch fehlgeschlagen: rule={}, key={}",
                        rule.getName(), context.triggerKey(), ex);
            }
        }
        return new AlertOutcome(gefeuert, unterdrueckt);
    }

    private boolean sollUnterdruecken(AlertRule rule, String key, Instant jetzt) {
        Optional<AlertEvent> letzte = eventRepository
                .findByRuleIdAndTriggerKey(rule.getId(), key);
        if (letzte.isEmpty()) {
            return false;
        }
        Duration seitLetzter = Duration.between(letzte.get().getLastFiredAt(), jetzt);
        return seitLetzter.toMinutes() < rule.getCooldownMinutes();
    }

    private void merkeFeuern(java.util.UUID ruleId, String key, Instant jetzt) {
        AlertEvent event = eventRepository
                .findByRuleIdAndTriggerKey(ruleId, key)
                .orElseGet(() -> AlertEvent.builder()
                        .ruleId(ruleId)
                        .triggerKey(key)
                        .suppressedCount(0)
                        .lastFiredAt(jetzt)
                        .build());
        event.setLastFiredAt(jetzt);
        event.setSuppressedCount(0);
        eventRepository.save(event);
    }

    private void inkrementiereSuppressed(
            java.util.UUID ruleId, String key, Instant jetzt) {
        AlertEvent event = eventRepository
                .findByRuleIdAndTriggerKey(ruleId, key)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "AlertEvent fuer Cooldown nicht gefunden, obwohl Cooldown greift"));
        event.setSuppressedCount(event.getSuppressedCount() + 1);
        // last_fired_at NICHT aktualisieren -- der Cooldown laeuft weiter ab.
        eventRepository.save(event);
        log.debug("Alert unterdrueckt (Cooldown): rule={}, key={}, jetzt={}",
                ruleId, key, jetzt);
    }

    /** Ergebnis einer Evaluation: Anzahl gefeuerter und unterdrueckter Regeln. */
    public record AlertOutcome(int gefeuert, int unterdrueckt) {}
}
