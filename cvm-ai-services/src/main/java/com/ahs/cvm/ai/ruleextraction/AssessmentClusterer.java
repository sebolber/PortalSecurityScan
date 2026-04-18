package com.ahs.cvm.ai.ruleextraction;

import com.ahs.cvm.persistence.assessment.Assessment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * Deterministisches Clustering der APPROVED-Assessments fuer die
 * Regel-Extraktion (Iteration 17, CVM-42).
 *
 * <p>Feature-Schluessel = {@code severity | sortierte rationaleSourceFields
 * | kev | componentType}. Semantisches Clustering via RAG-Embeddings
 * bleibt offener Punkt.
 */
@Component
public class AssessmentClusterer {

    public static final int MIN_CLUSTER_SIZE = 5;
    public static final int MIN_DISTINCT_CVES = 3;

    public List<AssessmentCluster> cluster(List<Assessment> input) {
        Map<String, List<Assessment>> buckets = new HashMap<>();
        for (Assessment a : input) {
            String key = featureKey(a);
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(a);
        }
        List<AssessmentCluster> result = new ArrayList<>();
        for (Map.Entry<String, List<Assessment>> e : buckets.entrySet()) {
            List<Assessment> members = e.getValue();
            if (members.size() < MIN_CLUSTER_SIZE) {
                continue;
            }
            Set<String> distinctCves = new HashSet<>();
            for (Assessment a : members) {
                if (a.getCve() != null && a.getCve().getCveId() != null) {
                    distinctCves.add(a.getCve().getCveId());
                }
            }
            if (distinctCves.size() < MIN_DISTINCT_CVES) {
                continue;
            }
            result.add(new AssessmentCluster(
                    e.getKey(),
                    List.copyOf(members),
                    List.copyOf(new TreeSet<>(distinctCves))));
        }
        result.sort((a, b) -> Integer.compare(
                b.assessments().size(), a.assessments().size()));
        return List.copyOf(result);
    }

    static String featureKey(Assessment a) {
        String sev = a.getSeverity() == null ? "-" : a.getSeverity().name();
        List<String> sortedFields = new ArrayList<>(
                a.getRationaleSourceFields() == null
                        ? List.of() : a.getRationaleSourceFields());
        sortedFields.sort(String::compareTo);
        String kev = (a.getCve() != null && Boolean.TRUE.equals(a.getCve().getKevListed()))
                ? "KEV" : "NON_KEV";
        String componentType = "-";
        if (a.getFinding() != null
                && a.getFinding().getComponentOccurrence() != null
                && a.getFinding().getComponentOccurrence().getComponent() != null
                && a.getFinding().getComponentOccurrence().getComponent().getType() != null) {
            componentType = a.getFinding().getComponentOccurrence().getComponent().getType();
        }
        return sev + "|" + String.join(",", sortedFields) + "|" + kev + "|" + componentType;
    }

    public record AssessmentCluster(
            String featureKey,
            List<Assessment> assessments,
            List<String> distinctCveKeys) {}
}
