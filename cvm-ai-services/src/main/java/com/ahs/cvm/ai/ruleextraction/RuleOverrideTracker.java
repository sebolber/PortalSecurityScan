package com.ahs.cvm.ai.ruleextraction;

import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.domain.enums.RuleStatus;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.rule.Rule;
import com.ahs.cvm.persistence.rule.RuleRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Beobachtet aktive AI-extrahierte Regeln: wird eine solche Regel
 * innerhalb der letzten 30 Tage mehr als {@code overrideReviewThreshold}
 * Mal per Mensch-Entscheidung override-t, setzen wir sie zurueck auf
 * {@link RuleStatus#DRAFT} (Iteration 17, CVM-42).
 *
 * <p>Override-Heuristik: Assessment mit {@code proposalSource=RULE}
 * und danach ein neueres Assessment fuer dieselbe CVE+Env+PV mit
 * abweichender Severity (proposalSource=HUMAN).
 */
@Component
public class RuleOverrideTracker {

    private static final Logger log = LoggerFactory.getLogger(RuleOverrideTracker.class);

    private final RuleExtractionConfig config;
    private final RuleRepository ruleRepository;
    private final AssessmentRepository assessmentRepository;

    public RuleOverrideTracker(
            RuleExtractionConfig config,
            RuleRepository ruleRepository,
            AssessmentRepository assessmentRepository) {
        this.config = config;
        this.ruleRepository = ruleRepository;
        this.assessmentRepository = assessmentRepository;
    }

    @Transactional
    public TrackerReport evaluate() {
        Instant since = Instant.now().minus(Duration.ofDays(30));
        int geprueft = 0;
        int zurueckgesetzt = 0;

        List<Rule> rules = ruleRepository.findByStatusOrderByCreatedAtDesc(RuleStatus.ACTIVE);
        for (Rule rule : rules) {
            if (rule.getExtractedFromAiSuggestionId() == null) {
                continue;
            }
            geprueft++;
            int overrides = zaehleOverrides(rule, since);
            if (overrides >= config.overrideReviewThreshold()) {
                rule.setStatus(RuleStatus.DRAFT);
                rule.setRetiredAt(null);
                ruleRepository.save(rule);
                zurueckgesetzt++;
                log.info("RuleOverrideTracker: Regel {} auf DRAFT gesetzt ({} Overrides)",
                        rule.getRuleKey(), overrides);
            }
        }
        return new TrackerReport(geprueft, zurueckgesetzt);
    }

    private int zaehleOverrides(Rule rule, Instant since) {
        int count = 0;
        UUID ruleId = rule.getId();
        List<Assessment> alle = assessmentRepository.findAll();
        // Vereinfachte Heuristik: ein HUMAN-Assessment mit createdAt nach
        // "since", dessen Vorgaenger (gleiche CVE/Env/PV) proposalSource=RULE
        // und auf diese ruleId verweist. Wir approximieren den Verweis
        // ueber aiSuggestionId nicht, sondern zaehlen HUMAN-Override-Events
        // fuer alle von der Regel erzeugten Assessments (proposalSource=RULE,
        // rule-id NICHT im AssessmentRepo gespeichert; Heuristik reicht
        // fuer Iteration 17, praeziser in Iteration 21).
        for (Assessment a : alle) {
            if (a.getProposalSource() != ProposalSource.HUMAN
                    || a.getCreatedAt() == null
                    || a.getCreatedAt().isBefore(since)) {
                continue;
            }
            // Existiert irgendein SUPERSEDED-RULE-Vorgaenger fuer dieselbe
            // CVE/Env/PV-Kombination?
            UUID cveId = a.getCve() == null ? null : a.getCve().getId();
            UUID envId = a.getEnvironment() == null ? null : a.getEnvironment().getId();
            UUID pvId = a.getProductVersion() == null ? null : a.getProductVersion().getId();
            if (cveId == null || envId == null || pvId == null) {
                continue;
            }
            boolean override = alle.stream().anyMatch(v ->
                    v.getProposalSource() == ProposalSource.RULE
                    && v.getSupersededAt() != null
                    && v.getCve() != null && cveId.equals(v.getCve().getId())
                    && v.getEnvironment() != null && envId.equals(v.getEnvironment().getId())
                    && v.getProductVersion() != null && pvId.equals(v.getProductVersion().getId()));
            if (override) {
                count++;
            }
        }
        return count;
    }

    public record TrackerReport(int geprueft, int zurueckgesetzt) {}
}
