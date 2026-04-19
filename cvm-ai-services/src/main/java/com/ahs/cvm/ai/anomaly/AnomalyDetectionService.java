package com.ahs.cvm.ai.anomaly;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.MitigationStrategy;
import com.ahs.cvm.persistence.anomaly.AnomalyEvent;
import com.ahs.cvm.persistence.anomaly.AnomalyEventRepository;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.mitigation.MitigationPlan;
import com.ahs.cvm.persistence.mitigation.MitigationPlanRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministische Anomalie-Pattern-Checks (Iteration 18, CVM-43).
 *
 * <p><strong>Invariante:</strong> Der Service aendert keine
 * Assessments. Er schreibt ausschliesslich neue
 * {@link AnomalyEvent}-Eintraege. {@code AnomalyDetectionServiceTest}
 * prueft das per Mock-verify({@code never}).
 */
@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);

    public static final String PATTERN_KEV_NA = "KEV_NOT_APPLICABLE";
    public static final String PATTERN_MANY_WAIVER = "MANY_ACCEPT_RISK";
    public static final String PATTERN_SIMILAR_REJ = "SIMILAR_TO_REJECTED";
    public static final String PATTERN_BIG_DOWNGRADE = "BIG_DOWNGRADE_WITHOUT_RULE";

    private final AnomalyConfig config;
    private final AssessmentRepository assessmentRepository;
    private final MitigationPlanRepository mitigationRepository;
    private final AnomalyEventRepository anomalyRepository;

    public AnomalyDetectionService(
            AnomalyConfig config,
            AssessmentRepository assessmentRepository,
            MitigationPlanRepository mitigationRepository,
            AnomalyEventRepository anomalyRepository) {
        this.config = config;
        this.assessmentRepository = assessmentRepository;
        this.mitigationRepository = mitigationRepository;
        this.anomalyRepository = anomalyRepository;
    }

    @Transactional
    public List<AnomalyEvent> check(Instant since) {
        if (!config.enabledEffective()) {
            return List.of();
        }
        List<Assessment> kandidaten = assessmentRepository.findAll().stream()
                .filter(a -> a.getCreatedAt() != null
                        && a.getCreatedAt().isAfter(since))
                .toList();
        List<Assessment> alle = assessmentRepository.findAll();

        List<AnomalyEvent> neu = new ArrayList<>();
        Map<String, Integer> waiverCount = waiverProBewerter(alle, since);
        List<Assessment> rejected = alle.stream()
                .filter(a -> a.getStatus() == AssessmentStatus.REJECTED)
                .toList();

        for (Assessment a : kandidaten) {
            pruefeKevNotApplicable(a).ifPresent(neu::add);
            pruefeManyWaiver(a, waiverCount).ifPresent(neu::add);
            pruefeSimilarToRejected(a, rejected).ifPresent(neu::add);
            pruefeDowngradeOhneRegel(a, alle).ifPresent(neu::add);
        }
        return neu.stream().map(this::persistWennNeu).toList();
    }

    private Optional<AnomalyEvent> pruefeKevNotApplicable(Assessment a) {
        if (a.getStatus() != AssessmentStatus.APPROVED) {
            return Optional.empty();
        }
        if (a.getSeverity() != AhsSeverity.NOT_APPLICABLE) {
            return Optional.empty();
        }
        if (a.getCve() == null) {
            return Optional.empty();
        }
        boolean kev = Boolean.TRUE.equals(a.getCve().getKevListed());
        BigDecimal epss = Optional.ofNullable(a.getCve().getEpssScore())
                .orElse(BigDecimal.ZERO);
        if (kev && epss.doubleValue() > config.kevEpssThresholdEffective()) {
            return Optional.of(eventFor(a, PATTERN_KEV_NA, "WARNING",
                    "KEV-gelistet + EPSS=" + epss + " aber NOT_APPLICABLE eingestuft.",
                    List.of("cve.kevListed=true", "cve.epssScore=" + epss)));
        }
        return Optional.empty();
    }

    private Optional<AnomalyEvent> pruefeManyWaiver(
            Assessment a, Map<String, Integer> waiverCount) {
        if (a.getDecidedBy() == null) {
            return Optional.empty();
        }
        int count = waiverCount.getOrDefault(a.getDecidedBy(), 0);
        if (count < config.manyAcceptRiskThresholdEffective()) {
            return Optional.empty();
        }
        List<MitigationPlan> plans = mitigationRepository.findByAssessmentId(a.getId());
        boolean acceptRisk = plans.stream().anyMatch(
                p -> p.getStrategy() == MitigationStrategy.ACCEPT_RISK);
        if (!acceptRisk) {
            return Optional.empty();
        }
        return Optional.of(eventFor(a, PATTERN_MANY_WAIVER, "WARNING",
                "Bewerter " + a.getDecidedBy() + " hat " + count
                        + " ACCEPT_RISK-Waiver in 24 h.",
                List.of("decidedBy=" + a.getDecidedBy(), "count=" + count)));
    }

    private Optional<AnomalyEvent> pruefeSimilarToRejected(
            Assessment a, List<Assessment> rejected) {
        if (a.getRationale() == null || a.getRationale().isBlank()) {
            return Optional.empty();
        }
        String text = normalize(a.getRationale());
        for (Assessment r : rejected) {
            if (r.getRationale() == null) {
                continue;
            }
            double sim = jaccard(text, normalize(r.getRationale()));
            if (sim >= config.similarRejectionThresholdEffective()) {
                return Optional.of(eventFor(a, PATTERN_SIMILAR_REJ, "INFO",
                        "Rationale sehr aehnlich zu einem bereits abgelehnten "
                                + "Vorschlag (Jaccard " + String.format("%.2f", sim) + ").",
                        List.of("rejectedAssessmentId=" + r.getId())));
            }
        }
        return Optional.empty();
    }

    private Optional<AnomalyEvent> pruefeDowngradeOhneRegel(
            Assessment a, List<Assessment> alle) {
        if (a.getStatus() != AssessmentStatus.APPROVED) {
            return Optional.empty();
        }
        if (a.getSeverity() == null) {
            return Optional.empty();
        }
        // Vorgaenger-Severity suchen (selbe finding-id, kleinere version).
        UUID findingId = a.getFinding() == null ? null : a.getFinding().getId();
        if (findingId == null) {
            return Optional.empty();
        }
        Optional<Assessment> vorgaenger = alle.stream()
                .filter(v -> v.getFinding() != null
                        && findingId.equals(v.getFinding().getId())
                        && v.getVersion() != null
                        && a.getVersion() != null
                        && v.getVersion() < a.getVersion())
                .max(Comparator.comparing(Assessment::getVersion));
        if (vorgaenger.isEmpty() || vorgaenger.get().getSeverity() == null) {
            return Optional.empty();
        }
        int von = vorgaenger.get().getSeverity().ordinal();
        int nach = a.getSeverity().ordinal();
        if (nach - von < 2) {
            return Optional.empty();
        }
        if (a.getProposalSource() != null
                && a.getProposalSource().name().equals("RULE")) {
            return Optional.empty();
        }
        return Optional.of(eventFor(a, PATTERN_BIG_DOWNGRADE, "CRITICAL",
                "Downgrade " + vorgaenger.get().getSeverity() + " -> " + a.getSeverity()
                        + " ohne RULE-Stuetze.",
                List.of("vorgaenger=" + vorgaenger.get().getId())));
    }

    private AnomalyEvent persistWennNeu(AnomalyEvent event) {
        if (anomalyRepository.existsByAssessmentIdAndPattern(
                event.getAssessmentId(), event.getPattern())) {
            log.debug("AnomalyEvent {} / {} schon vorhanden - skip.",
                    event.getAssessmentId(), event.getPattern());
            return event;
        }
        return anomalyRepository.save(event);
    }

    private AnomalyEvent eventFor(Assessment a, String pattern, String severity,
            String reason, List<String> pointers) {
        return AnomalyEvent.builder()
                .assessmentId(a.getId())
                .pattern(pattern)
                .severity(severity)
                .reason(reason)
                .pointersJson("[\"" + String.join("\",\"", pointers) + "\"]")
                .triggeredAt(Instant.now())
                .build();
    }

    private Map<String, Integer> waiverProBewerter(List<Assessment> alle, Instant since) {
        Map<String, Integer> map = new HashMap<>();
        for (Assessment a : alle) {
            if (a.getDecidedBy() == null || a.getDecidedAt() == null) {
                continue;
            }
            if (!a.getDecidedAt().isAfter(since)) {
                continue;
            }
            List<MitigationPlan> plans = mitigationRepository.findByAssessmentId(a.getId());
            boolean acceptRisk = plans.stream().anyMatch(
                    p -> p.getStrategy() == MitigationStrategy.ACCEPT_RISK);
            if (acceptRisk) {
                map.merge(a.getDecidedBy(), 1, Integer::sum);
            }
        }
        return map;
    }

    static String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    static double jaccard(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }
        java.util.Set<String> setA = new java.util.HashSet<>(List.of(a.split(" ")));
        java.util.Set<String> setB = new java.util.HashSet<>(List.of(b.split(" ")));
        if (setA.isEmpty() && setB.isEmpty()) {
            return 1.0;
        }
        java.util.Set<String> union = new java.util.HashSet<>(setA);
        union.addAll(setB);
        java.util.Set<String> intersection = new java.util.HashSet<>(setA);
        intersection.retainAll(setB);
        return (double) intersection.size() / (double) union.size();
    }

    /** Gibt Duration {@code 24h} zurueck. Separater Accessor fuer Tests. */
    public Duration standardFenster() {
        return Duration.ofHours(24);
    }
}
