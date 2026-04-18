package com.ahs.cvm.application.modelprofile;

import com.ahs.cvm.persistence.ai.AiCallAuditRepository;
import com.ahs.cvm.persistence.modelprofile.LlmModelProfile;
import com.ahs.cvm.persistence.modelprofile.LlmModelProfileRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Vergleicht die aufgelaufenen KI-Kosten eines Modells im laufenden
 * Monat gegen das hinterlegte Budget (Iteration 21, CVM-52).
 *
 * <p>Wird von den KI-Services vor dem Call konsultiert: ist das
 * Budget gerissen, liefert {@link #isUnderBudget(UUID)} {@code false}
 * und der Service faellt in den Regel-only-Modus zurueck.
 */
@Service
public class LlmCostGuard {

    private static final Logger log = LoggerFactory.getLogger(LlmCostGuard.class);

    private final LlmModelProfileRepository profileRepository;
    private final AiCallAuditRepository auditRepository;
    private final Clock clock;

    public LlmCostGuard(
            LlmModelProfileRepository profileRepository,
            AiCallAuditRepository auditRepository,
            Clock clock) {
        this.profileRepository = profileRepository;
        this.auditRepository = auditRepository;
        this.clock = clock;
    }

    /**
     * Gibt zurueck, ob das Monatsbudget fuer das uebergebene
     * Modell-Profil noch nicht aufgebraucht ist. {@code null}-Profil
     * oder Budget &lt;= 0 bedeuten "unbegrenzt".
     */
    public boolean isUnderBudget(UUID modelProfileId) {
        if (modelProfileId == null) {
            return true;
        }
        LlmModelProfile profile = profileRepository.findById(modelProfileId)
                .orElse(null);
        if (profile == null) {
            log.warn("Unbekanntes LlmModelProfile {} - behandle als unbegrenzt.",
                    modelProfileId);
            return true;
        }
        BigDecimal budget = profile.getCostBudgetEurMonthly();
        if (budget == null || budget.signum() <= 0) {
            return true;
        }
        Range r = rangeForCurrentMonth();
        BigDecimal sum = auditRepository.sumCostEurForModelAndRange(
                profile.getModelId(), r.from(), r.to());
        if (sum == null) {
            sum = BigDecimal.ZERO;
        }
        boolean under = sum.compareTo(budget) < 0;
        if (!under) {
            log.warn("Kosten-Cap erreicht fuer Modell {} im {}: {} EUR / Budget {} EUR",
                    profile.getModelId(), r.yearMonth(), sum, budget);
        }
        return under;
    }

    /** Paketsichtbar fuer Tests. */
    Range rangeForCurrentMonth() {
        Instant now = Instant.now(clock);
        YearMonth ym = YearMonth.from(now.atZone(ZoneOffset.UTC));
        Instant from = ym.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = ym.plusMonths(1).atDay(1).atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        return new Range(from, to, ym);
    }

    record Range(Instant from, Instant to, YearMonth yearMonth) {}
}
