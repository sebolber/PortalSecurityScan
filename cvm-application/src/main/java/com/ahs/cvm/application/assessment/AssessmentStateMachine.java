package com.ahs.cvm.application.assessment;

import com.ahs.cvm.domain.enums.AssessmentStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Zentrale Statusmaschine fuer {@link AssessmentStatus}. Kein Spring-Kontext
 * noetig &mdash; wird aber als Bean registriert, damit Services sie per DI
 * einbinden koennen.
 *
 * <p>Uebergangs-Tabelle (Konzept v0.2, Abschnitt 6.2):
 * <pre>
 *   PROPOSED      -&gt; APPROVED | REJECTED | NEEDS_REVIEW | SUPERSEDED
 *   APPROVED      -&gt; EXPIRED  | SUPERSEDED
 *   NEEDS_REVIEW  -&gt; PROPOSED | SUPERSEDED
 *   REJECTED, SUPERSEDED, EXPIRED  -&gt; terminal
 * </pre>
 */
@Component
public class AssessmentStateMachine {

    private static final Map<AssessmentStatus, Set<AssessmentStatus>> ERLAUBT;

    static {
        Map<AssessmentStatus, Set<AssessmentStatus>> map = new EnumMap<>(AssessmentStatus.class);
        map.put(
                AssessmentStatus.PROPOSED,
                EnumSet.of(
                        AssessmentStatus.APPROVED,
                        AssessmentStatus.REJECTED,
                        AssessmentStatus.NEEDS_REVIEW,
                        AssessmentStatus.SUPERSEDED));
        map.put(
                AssessmentStatus.APPROVED,
                EnumSet.of(AssessmentStatus.EXPIRED, AssessmentStatus.SUPERSEDED));
        map.put(
                AssessmentStatus.NEEDS_REVIEW,
                EnumSet.of(AssessmentStatus.PROPOSED, AssessmentStatus.SUPERSEDED));
        map.put(
                AssessmentStatus.NEEDS_VERIFICATION,
                EnumSet.of(
                        AssessmentStatus.APPROVED,
                        AssessmentStatus.REJECTED,
                        AssessmentStatus.PROPOSED,
                        AssessmentStatus.SUPERSEDED));
        map.put(AssessmentStatus.REJECTED, EnumSet.noneOf(AssessmentStatus.class));
        map.put(AssessmentStatus.SUPERSEDED, EnumSet.noneOf(AssessmentStatus.class));
        map.put(AssessmentStatus.EXPIRED, EnumSet.noneOf(AssessmentStatus.class));
        ERLAUBT = Map.copyOf(map);
    }

    public void pruefeUebergang(AssessmentStatus von, AssessmentStatus nach) {
        if (!ERLAUBT.getOrDefault(von, EnumSet.noneOf(AssessmentStatus.class)).contains(nach)) {
            throw new InvalidAssessmentTransitionException(von, nach);
        }
    }

    public boolean isTerminal(AssessmentStatus status) {
        return ERLAUBT.getOrDefault(status, EnumSet.noneOf(AssessmentStatus.class)).isEmpty();
    }
}
