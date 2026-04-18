package com.ahs.cvm.ai.ruleextraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.cve.Cve;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssessmentClustererTest {

    private final AssessmentClusterer clusterer = new AssessmentClusterer();

    private Assessment assessment(String cveKey, AhsSeverity severity, List<String> fields) {
        Cve cve = Cve.builder().id(UUID.randomUUID()).cveId(cveKey).kevListed(false).build();
        return Assessment.builder()
                .id(UUID.randomUUID())
                .cve(cve)
                .severity(severity)
                .rationaleSourceFields(fields)
                .build();
    }

    @Test
    @DisplayName("Regel-Extraktion: Cluster mit weniger als fuenf Assessments erzeugt keinen Vorschlag")
    void mindestgroesse() {
        var clusters = clusterer.cluster(List.of(
                assessment("CVE-1", AhsSeverity.MEDIUM, List.of("a")),
                assessment("CVE-2", AhsSeverity.MEDIUM, List.of("a")),
                assessment("CVE-3", AhsSeverity.MEDIUM, List.of("a")),
                assessment("CVE-4", AhsSeverity.MEDIUM, List.of("a"))));
        assertThat(clusters).isEmpty();
    }

    @Test
    @DisplayName("Clusterer: mind. 5 Assessments + 3 distinkte CVEs -> 1 Cluster")
    void happyCluster() {
        var clusters = clusterer.cluster(List.of(
                assessment("CVE-1", AhsSeverity.MEDIUM, List.of("a")),
                assessment("CVE-2", AhsSeverity.MEDIUM, List.of("a")),
                assessment("CVE-3", AhsSeverity.MEDIUM, List.of("a")),
                assessment("CVE-1", AhsSeverity.MEDIUM, List.of("a")),
                assessment("CVE-2", AhsSeverity.MEDIUM, List.of("a"))));
        assertThat(clusters).hasSize(1);
        assertThat(clusters.get(0).assessments()).hasSize(5);
        assertThat(clusters.get(0).distinctCveKeys()).containsExactly("CVE-1", "CVE-2", "CVE-3");
    }

    @Test
    @DisplayName("Clusterer: unterschiedliche Severity -> unterschiedliche Cluster")
    void severityTrennen() {
        var clusters = clusterer.cluster(List.of(
                assessment("CVE-1", AhsSeverity.MEDIUM, List.of("a")),
                assessment("CVE-2", AhsSeverity.MEDIUM, List.of("a")),
                assessment("CVE-3", AhsSeverity.MEDIUM, List.of("a")),
                assessment("CVE-4", AhsSeverity.MEDIUM, List.of("a")),
                assessment("CVE-5", AhsSeverity.MEDIUM, List.of("a")),
                assessment("CVE-1", AhsSeverity.HIGH, List.of("a"))));
        assertThat(clusters).hasSize(1);
        assertThat(clusters.get(0).assessments())
                .allMatch(a -> a.getSeverity() == AhsSeverity.MEDIUM);
    }

    @Test
    @DisplayName("Clusterer: Feature-Key ist deterministisch - gleiche Eingabe -> gleiche Cluster")
    void deterministisch() {
        var input = List.of(
                assessment("CVE-1", AhsSeverity.LOW, List.of("b", "a")),
                assessment("CVE-2", AhsSeverity.LOW, List.of("a", "b")),
                assessment("CVE-3", AhsSeverity.LOW, List.of("a", "b")),
                assessment("CVE-1", AhsSeverity.LOW, List.of("a", "b")),
                assessment("CVE-2", AhsSeverity.LOW, List.of("a", "b")));
        assertThat(clusterer.cluster(input).get(0).featureKey())
                .isEqualTo(clusterer.cluster(input).get(0).featureKey());
    }
}
