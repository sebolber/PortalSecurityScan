package com.ahs.cvm.ai.ruleextraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.ai.ruleextraction.DryRunEvaluator.DryRunReport;
import com.ahs.cvm.application.rules.ConditionParser;
import com.ahs.cvm.application.rules.RuleEvaluator;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.cve.Cve;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DryRunEvaluatorTest {

    private final DryRunEvaluator evaluator = new DryRunEvaluator(
            new ConditionParser(), new RuleEvaluator());

    private Assessment ahs(String cveKey, boolean kev, AhsSeverity severity) {
        Cve cve = Cve.builder()
                .id(UUID.randomUUID())
                .cveId(cveKey)
                .kevListed(kev)
                .cvssBaseScore(BigDecimal.valueOf(kev ? 9.0 : 4.0))
                .build();
        return Assessment.builder()
                .id(UUID.randomUUID())
                .cve(cve)
                .severity(severity)
                .rationaleSourceFields(List.of())
                .build();
    }

    private static final String KEV_CONDITION = """
            {"all":[{"eq":{"path":"cve.kev","value":true}}]}""";

    @Test
    @DisplayName("DryRun: leere Historie -> alles 0")
    void leereHistorie() {
        DryRunReport r = evaluator.evaluate(KEV_CONDITION, AhsSeverity.CRITICAL, List.of());
        assertThat(r.historicalMatchCount()).isZero();
        assertThat(r.wouldHaveCovered()).isZero();
        assertThat(r.coverageRate()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("DryRun: 100% Treffer, alle mit erwarteter Severity")
    void coverage100() {
        DryRunReport r = evaluator.evaluate(KEV_CONDITION, AhsSeverity.CRITICAL, List.of(
                ahs("CVE-1", true, AhsSeverity.CRITICAL),
                ahs("CVE-2", true, AhsSeverity.CRITICAL),
                ahs("CVE-3", false, AhsSeverity.MEDIUM)));
        assertThat(r.historicalMatchCount()).isEqualTo(2);
        assertThat(r.wouldHaveCovered()).isEqualTo(2);
        assertThat(r.coverageRate()).isEqualByComparingTo("1.000");
        assertThat(r.conflicts()).isEmpty();
    }

    @Test
    @DisplayName("DryRun: Konflikte werden gezaehlt und gelistet")
    void konflikte() {
        DryRunReport r = evaluator.evaluate(KEV_CONDITION, AhsSeverity.CRITICAL, List.of(
                ahs("CVE-1", true, AhsSeverity.CRITICAL),
                ahs("CVE-2", true, AhsSeverity.HIGH),
                ahs("CVE-3", true, AhsSeverity.MEDIUM)));
        assertThat(r.historicalMatchCount()).isEqualTo(3);
        assertThat(r.wouldHaveCovered()).isEqualTo(1);
        assertThat(r.conflicts()).hasSize(2);
    }

    @Test
    @DisplayName("DryRun: ungueltige Condition -> leerer Report")
    void invalidCondition() {
        DryRunReport r = evaluator.evaluate("nicht-json", AhsSeverity.LOW, List.of(
                ahs("CVE-1", false, AhsSeverity.LOW)));
        assertThat(r.historicalMatchCount()).isZero();
    }

    @Test
    @DisplayName("DryRun: keine Treffer -> rate bleibt 0")
    void nullTreffer() {
        DryRunReport r = evaluator.evaluate(KEV_CONDITION, AhsSeverity.LOW, List.of(
                ahs("CVE-1", false, AhsSeverity.LOW)));
        assertThat(r.historicalMatchCount()).isZero();
        assertThat(r.coverageRate()).isEqualByComparingTo("0");
    }
}
