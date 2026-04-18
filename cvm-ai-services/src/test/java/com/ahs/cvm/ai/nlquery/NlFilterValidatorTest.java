package com.ahs.cvm.ai.nlquery;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NlFilterValidatorTest {

    private final NlFilterValidator validator = new NlFilterValidator();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("NL-Query: gueltiger Filter mit bekannten Feldern wird akzeptiert")
    void gueltigerFilter() throws Exception {
        var out = mapper.readTree("""
                {"filter":{"environment":"PROD","severityIn":["HIGH"],
                  "minAgeDays":30,"hasUpstreamFix":true},
                 "sortBy":"age_desc","explanation":"ok"}""");

        var r = validator.validate(out);

        assertThat(r.ok()).isTrue();
        assertThat(r.filter().environmentKey()).isEqualTo("PROD");
        assertThat(r.filter().severityIn()).containsExactly(AhsSeverity.HIGH);
        assertThat(r.filter().minAgeDays()).isEqualTo(30);
        assertThat(r.filter().sortBy()).isEqualTo(NlFilter.SortBy.AGE_DESC);
    }

    @Test
    @DisplayName("NL-Query: unbekanntes Filterfeld wird abgelehnt, kein SQL entsteht")
    void unbekanntesFeld() throws Exception {
        var out = mapper.readTree("""
                {"filter":{"environment":"PROD","deleteAll":true},
                 "sortBy":"age_desc","explanation":""}""");

        var r = validator.validate(out);

        assertThat(r.ok()).isFalse();
        assertThat(r.errors()).anyMatch(e -> e.contains("deleteAll"));
    }

    @Test
    @DisplayName("NL-Query: unbekannter Sort-Wert wird abgelehnt")
    void unbekannterSort() throws Exception {
        var out = mapper.readTree("""
                {"filter":{},"sortBy":"drop_table","explanation":""}""");

        var r = validator.validate(out);

        assertThat(r.ok()).isFalse();
        assertThat(r.errors()).anyMatch(e -> e.contains("Sortier"));
    }

    @Test
    @DisplayName("NL-Query: unbekannter Severity-Wert wird als Fehler gemeldet")
    void unbekannteSeverity() throws Exception {
        var out = mapper.readTree("""
                {"filter":{"severityIn":["CATASTROPHIC"]},
                 "sortBy":"age_desc","explanation":""}""");

        var r = validator.validate(out);

        assertThat(r.ok()).isFalse();
        assertThat(r.errors()).anyMatch(e -> e.contains("CATASTROPHIC"));
    }

    @Test
    @DisplayName("NL-Query: Status-Whitelist akzeptiert neue NEEDS_VERIFICATION")
    void statusOk() throws Exception {
        var out = mapper.readTree("""
                {"filter":{"statusIn":["PROPOSED","NEEDS_VERIFICATION"]},
                 "sortBy":"age_asc","explanation":""}""");

        var r = validator.validate(out);

        assertThat(r.ok()).isTrue();
        assertThat(r.filter().statusIn()).containsExactly(
                AssessmentStatus.PROPOSED, AssessmentStatus.NEEDS_VERIFICATION);
    }

    @Test
    @DisplayName("NL-Query: null/empty Output -> fail")
    void ungueltigerOutput() throws Exception {
        var r = validator.validate(mapper.readTree("\"nope\""));
        assertThat(r.ok()).isFalse();
    }
}
