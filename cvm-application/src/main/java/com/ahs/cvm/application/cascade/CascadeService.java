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
 * REUSE &rarr; RULE &rarr; (AI-Platzhalter) &rarr; MANUAL.
 *
 * <p>Der Service schreibt <em>nichts</em> &mdash; er liefert nur den
 * Outcome. Die Persistenz-Anbindung folgt in Iteration 06.
 */
@Service
public class CascadeService {

    private static final Logger log = LoggerFactory.getLogger(CascadeService.class);

    private final AssessmentLookupService lookupService;
    private final RuleEngine ruleEngine;

    public CascadeService(AssessmentLookupService lookupService, RuleEngine ruleEngine) {
        this.lookupService = lookupService;
        this.ruleEngine = ruleEngine;
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

        // Stufe 3: AI (Platzhalter, Iteration 13)
        // Stufe 4: MANUAL
        return CascadeOutcome.manual();
    }
}
