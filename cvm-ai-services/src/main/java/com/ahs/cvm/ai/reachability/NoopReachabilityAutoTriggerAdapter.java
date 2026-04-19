package com.ahs.cvm.ai.reachability;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default-Implementierung des {@link ReachabilityAutoTriggerPort}
 * fuer Iteration 70. Loggt den Auto-Trigger-Wunsch nur, ohne den
 * Subprocess zu starten - das uebernimmt der JGit-gestuetzte
 * Adapter in Iteration 71.
 *
 * <p>Der {@code @ConditionalOnMissingBean}-Schutz erlaubt es dem
 * naechsten Iterationsschritt, einfach eine konkrete Implementierung
 * zu liefern und diesen Noop-Fallback zu verdraengen.
 */
@Component
@ConditionalOnMissingBean(ReachabilityAutoTriggerPort.class)
public class NoopReachabilityAutoTriggerAdapter
        implements ReachabilityAutoTriggerPort {

    private static final Logger log = LoggerFactory.getLogger(
            NoopReachabilityAutoTriggerAdapter.class);

    @Override
    public void trigger(UUID findingId, String triggeredBy) {
        log.info(
                "Reachability-Auto-Trigger (Noop) fuer finding={} durch {}",
                findingId,
                triggeredBy);
    }
}
