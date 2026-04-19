package com.ahs.cvm.ai.reachability;

import com.ahs.cvm.ai.autoassessment.LowConfidenceAiSuggestionEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Empfaengt {@link LowConfidenceAiSuggestionEvent}s, prueft
 * Schwellwert und In-Memory-Rate-Limit und delegiert bei Treffer
 * an den {@link ReachabilityAutoTriggerPort} (Iteration 70,
 * CVM-307).
 *
 * <p>Der Listener laeuft <em>nach</em> erfolgreichem Commit der
 * AI-Vorschlag-Transaktion
 * ({@link TransactionPhase#AFTER_COMMIT}), damit die Suggestion
 * bereits persistiert ist, falls der Port sie nachschlaegt.
 *
 * <p>Rate-Limit-Schluessel ist {@code (productVersionId, cveKey)}.
 * Ohne {@code productVersionId} (z.&nbsp;B. in alten Events) wird
 * per Feature-Kompatibilitaet auf {@code (null, cveKey)} reduziert.
 * Die Cooldown-Minuten stammen aus
 * {@link ReachabilityConfig#autoTriggerCooldownMinutesEffective()}.
 */
@Service
public class ReachabilityAutoTriggerService {

    private static final Logger log = LoggerFactory.getLogger(
            ReachabilityAutoTriggerService.class);

    private final ReachabilityConfig config;
    private final ReachabilityAutoTriggerPort port;
    private final Clock clock;
    private final Map<RateLimitKey, Instant> letzteTrigger = new ConcurrentHashMap<>();

    @Autowired
    public ReachabilityAutoTriggerService(
            ReachabilityConfig config,
            ReachabilityAutoTriggerPort port,
            Clock clock) {
        this.config = config;
        this.port = port;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public ReachabilityAutoTriggerService(
            ReachabilityConfig config,
            ReachabilityAutoTriggerPort port) {
        this(config, port, Clock.systemUTC());
    }

    /**
     * Fallback fuer Nicht-Transaction-Tests: nutzt die
     * Standard-Event-API. In Prod greift
     * {@link #onLowConfidenceTransactional(LowConfidenceAiSuggestionEvent)}
     * dank {@code @TransactionalEventListener} zuerst.
     */
    @EventListener
    public void onLowConfidence(LowConfidenceAiSuggestionEvent event) {
        consider(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLowConfidenceTransactional(LowConfidenceAiSuggestionEvent event) {
        consider(event);
    }

    boolean consider(LowConfidenceAiSuggestionEvent event) {
        if (!config.enabledEffective()) {
            log.debug("Auto-Trigger ueberspringe: Reachability deaktiviert.");
            return false;
        }
        BigDecimal schwelle = config.autoTriggerThresholdEffective();
        if (event.confidence().compareTo(schwelle) >= 0) {
            log.debug(
                    "Auto-Trigger ueberspringe: confidence={} >= threshold={}",
                    event.confidence(),
                    schwelle);
            return false;
        }
        Duration cooldown = Duration.ofMinutes(
                config.autoTriggerCooldownMinutesEffective());
        Instant now = clock.instant();
        RateLimitKey key = new RateLimitKey(event.productVersionId(), event.cveKey());
        Instant letzter = letzteTrigger.get(key);
        if (letzter != null && !cooldown.isZero()
                && Duration.between(letzter, now).compareTo(cooldown) < 0) {
            log.info(
                    "Auto-Trigger unterdrueckt (Cooldown): finding={}, pv={}, cve={}",
                    event.findingId(),
                    event.productVersionId(),
                    event.cveKey());
            return false;
        }
        letzteTrigger.put(key, now);
        log.info(
                "Auto-Trigger Reachability: finding={}, confidence={} < threshold={}",
                event.findingId(),
                event.confidence(),
                schwelle);
        port.trigger(event.findingId(), event.triggeredBy());
        return true;
    }

    /** Test-Helper: Rate-Limit-Cache leeren. */
    void reset() {
        letzteTrigger.clear();
    }

    private record RateLimitKey(UUID productVersionId, String cveKey) {
        private RateLimitKey {
            Objects.requireNonNull(cveKey, "cveKey");
        }
    }
}
