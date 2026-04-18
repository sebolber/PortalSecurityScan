package com.ahs.cvm.application.cascade;

import com.ahs.cvm.application.assessment.AssessmentLookupService;
import com.ahs.cvm.application.rules.ProposedResult;
import com.ahs.cvm.application.rules.RuleEngine;
import com.ahs.cvm.persistence.assessment.Assessment;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cascade-Stufen in der Reihenfolge aus Konzept v0.2 Abschnitt 4.3:
 * REUSE &rarr; RULE &rarr; AI &rarr; MANUAL.
 *
 * <p>Stufe&nbsp;3 (AI) wird ueber den optionalen
 * {@link AiAssessmentSuggesterPort} angesprochen. Ist keine Bean
 * registriert (Default in Tests / im Stand vor Iteration 13), bleibt
 * der Service auf MANUAL. So bleibt die Modulgrenze
 * {@code application -&gt; domain, persistence} (CLAUDE.md
 * Abschnitt 3) gewahrt.
 */
@Service
public class CascadeService {

    private static final Logger log = LoggerFactory.getLogger(CascadeService.class);

    private final AssessmentLookupService lookupService;
    private final RuleEngine ruleEngine;
    private final Optional<AiAssessmentSuggesterPort> aiSuggester;

    public CascadeService(
            AssessmentLookupService lookupService,
            RuleEngine ruleEngine,
            Optional<AiAssessmentSuggesterPort> aiSuggester) {
        this.lookupService = lookupService;
        this.ruleEngine = ruleEngine;
        this.aiSuggester = aiSuggester;
    }

    @Transactional(readOnly = true)
    public CascadeOutcome bewerte(CascadeInput input) {
        // Stufe 1: REUSE
        List<Assessment> bestehende = lookupService.findeAktiveFreigaben(
                input.cveId(), input.productVersionId(), input.environmentId());
        if (!bestehende.isEmpty()) {
            Assessment reuse = bestehende.get(0);
            log.debug("Cascade: REUSE-Treffer fuer CVE {} / PV {} / env {}",
                    input.cveId(), input.productVersionId(), input.environmentId());
            return CascadeOutcome.reuse(reuse.getId(), reuse.getSeverity(), reuse.getRationale());
        }

        // Stufe 2: RULE
        Optional<ProposedResult> rule = ruleEngine.evaluate(input.evaluationContext());
        if (rule.isPresent()) {
            ProposedResult r = rule.get();
            log.debug("Cascade: Regel {} trifft", r.ruleKey());
            return CascadeOutcome.rule(r.ruleId(), r.severity(), r.rationale(), r.sourceFields());
        }

        // Stufe 3: AI (optional, Iteration 13)
        if (aiSuggester.isPresent()) {
            try {
                Optional<CascadeOutcome> ai = aiSuggester.get().suggest(input);
                if (ai.isPresent()) {
                    log.debug("Cascade: AI-Vorschlag fuer CVE {}", input.cveId());
                    return ai.get();
                }
            } catch (RuntimeException ex) {
                log.warn("Cascade-AI-Stufe fehlgeschlagen, faellt auf MANUAL: {}",
                        ex.getMessage());
            }
        }

        // Stufe 4: MANUAL
        return CascadeOutcome.manual();
    }
}
