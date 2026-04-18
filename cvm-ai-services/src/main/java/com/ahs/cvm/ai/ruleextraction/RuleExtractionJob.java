package com.ahs.cvm.ai.ruleextraction;

import com.ahs.cvm.ai.ruleextraction.AssessmentClusterer.AssessmentCluster;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.rulesuggestion.RuleSuggestion;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nightly-Job (02:30) fuer die Regel-Extraktion. Iteriert bis zu
 * {@link RuleExtractionConfig#clusterCap()} Cluster; darueber hinaus
 * werden Cluster protokolliert und in der naechsten Nacht bevorzugt
 * verarbeitet (Iteration 17, CVM-42).
 */
@Component
public class RuleExtractionJob {

    private static final Logger log = LoggerFactory.getLogger(RuleExtractionJob.class);

    private final RuleExtractionConfig config;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentClusterer clusterer;
    private final RuleExtractionService extractionService;
    private final boolean schedulerEnabled;

    public RuleExtractionJob(
            RuleExtractionConfig config,
            AssessmentRepository assessmentRepository,
            AssessmentClusterer clusterer,
            RuleExtractionService extractionService,
            @Value("${cvm.scheduler.enabled:false}") boolean schedulerEnabled) {
        this.config = config;
        this.assessmentRepository = assessmentRepository;
        this.clusterer = clusterer;
        this.extractionService = extractionService;
        this.schedulerEnabled = schedulerEnabled;
    }

    @Scheduled(cron = "${cvm.ai.rule-extraction.cron:0 30 2 * * *}")
    @Transactional
    public void scheduledRun() {
        if (!config.enabled() || !schedulerEnabled) {
            return;
        }
        runOnce();
    }

    /** Manueller Trigger fuer Tests / Admin-Endpoint. */
    @Transactional
    public JobReport runOnce() {
        Instant grenze = Instant.now().minus(Duration.ofDays(config.windowDays()));
        List<Assessment> historie = new ArrayList<>();
        for (Assessment a : assessmentRepository.findAll()) {
            if (a.getStatus() == AssessmentStatus.APPROVED
                    && a.getSupersededAt() == null
                    && a.getDecidedAt() != null
                    && a.getDecidedAt().isAfter(grenze)) {
                historie.add(a);
            }
        }
        if (historie.isEmpty()) {
            log.info("Rule-Extraction: keine APPROVED-Historie im Fenster.");
            return new JobReport(0, 0, 0);
        }
        List<AssessmentCluster> cluster = clusterer.cluster(historie);
        int processed = 0;
        int erzeugt = 0;
        int deferred = 0;
        for (AssessmentCluster c : cluster) {
            if (processed >= config.clusterCap()) {
                deferred++;
                continue;
            }
            processed++;
            var out = extractionService.extract(c, historie);
            if (out.isPresent()) {
                erzeugt++;
            }
        }
        log.info("Rule-Extraction: {} Cluster verarbeitet, {} Suggestions, {} deferred.",
                processed, erzeugt, deferred);
        return new JobReport(processed, erzeugt, deferred);
    }

    public record JobReport(int processed, int suggestionsCreated, int deferred) {}
}
