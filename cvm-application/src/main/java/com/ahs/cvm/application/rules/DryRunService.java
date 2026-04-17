package com.ahs.cvm.application.rules;

import com.ahs.cvm.application.profile.ContextProfileService;
import com.ahs.cvm.application.profile.ProfileView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.rule.Rule;
import com.ahs.cvm.persistence.rule.RuleDryRunResult;
import com.ahs.cvm.persistence.rule.RuleDryRunResultRepository;
import com.ahs.cvm.persistence.rule.RuleRepository;
import com.ahs.cvm.persistence.scan.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
 * Fuehrt eine Regel gegen historische Findings aus und liefert Coverage- und
 * Konflikt-Statistik. Ergebnis wird in {@code rule_dry_run_result}
 * persistiert, damit der Verlauf spaeter in der UI sichtbar bleibt.
 */
@Service
public class DryRunService {

    private static final Logger log = LoggerFactory.getLogger(DryRunService.class);
    private static final int MAX_DAYS = 3650;

    private final RuleRepository ruleRepository;
    private final FindingRepository findingRepository;
    private final AssessmentRepository assessmentRepository;
    private final RuleDryRunResultRepository dryRunResultRepository;
    private final ContextProfileService profileService;
    private final ConditionParser parser;
    private final RuleEvaluator evaluator;

    private final YAMLMapper yamlMapper = new YAMLMapper();

    public DryRunService(
            RuleRepository ruleRepository,
            FindingRepository findingRepository,
            AssessmentRepository assessmentRepository,
            RuleDryRunResultRepository dryRunResultRepository,
            ContextProfileService profileService,
            ConditionParser parser,
            RuleEvaluator evaluator) {
        this.ruleRepository = ruleRepository;
        this.findingRepository = findingRepository;
        this.assessmentRepository = assessmentRepository;
        this.dryRunResultRepository = dryRunResultRepository;
        this.profileService = profileService;
        this.parser = parser;
        this.evaluator = evaluator;
    }

    @Transactional
    public DryRunResult dryRun(UUID ruleId, int days) {
        if (days <= 0 || days > MAX_DAYS) {
            throw new IllegalArgumentException(
                    "days muss im Bereich 1.." + MAX_DAYS + " liegen (war: " + days + ").");
        }
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));
        ConditionNode condition = parser.parse(rule.getConditionJson());

        Instant end = Instant.now();
        Instant start = end.minus(Duration.ofDays(days));
        List<Finding> findings = findingRepository.findByDetectedAtBetween(start, end);

        Map<UUID, Optional<ProfileView>> profilCache = new HashMap<>();
        Map<UUID, JsonNode> profilBaumCache = new HashMap<>();

        int matched = 0;
        int matchedApproved = 0;
        List<DryRunResult.Conflict> conflicts = new ArrayList<>();

        for (Finding finding : findings) {
            JsonNode profil = ladeProfil(finding, profilCache, profilBaumCache);
            RuleEvaluationContext ctx = kontext(finding, profil);
            if (!evaluator.evaluate(condition, ctx)) {
                continue;
            }
            matched++;
            Optional<Assessment> approved = assessmentRepository
                    .findFirstByFindingIdOrderByVersionDesc(finding.getId())
                    .filter(a -> a.getStatus() == AssessmentStatus.APPROVED);
            if (approved.isPresent()) {
                matchedApproved++;
                AhsSeverity approvedSeverity = approved.get().getSeverity();
                if (approvedSeverity != rule.getProposedSeverity()) {
                    conflicts.add(new DryRunResult.Conflict(
                            finding.getId(),
                            approved.get().getId(),
                            approvedSeverity,
                            rule.getProposedSeverity()));
                }
            }
        }

        RuleDryRunResult persisted = RuleDryRunResult.builder()
                .rule(rule)
                .executedAt(Instant.now())
                .rangeStart(start)
                .rangeEnd(end)
                .totalFindings(findings.size())
                .matchedFindings(matched)
                .matchedAlreadyApproved(matchedApproved)
                .conflicts(konfliktJson(conflicts))
                .build();
        dryRunResultRepository.save(persisted);

        log.info(
                "Dry-Run Regel {}: {} Findings, {} Matches, {} approved, {} Konflikte",
                rule.getRuleKey(), findings.size(), matched, matchedApproved, conflicts.size());

        return new DryRunResult(
                ruleId, start, end, findings.size(), matched, matchedApproved, conflicts);
    }

    private JsonNode ladeProfil(
            Finding f,
            Map<UUID, Optional<ProfileView>> profilCache,
            Map<UUID, JsonNode> baumCache) {
        Environment env = f.getScan() == null ? null : f.getScan().getEnvironment();
        if (env == null) return null;
        UUID envId = env.getId();
        if (envId == null) return null;
        Optional<ProfileView> view = profilCache.computeIfAbsent(
                envId, profileService::latestActiveFor);
        if (view.isEmpty()) return null;
        return baumCache.computeIfAbsent(envId, id -> {
            try {
                return yamlMapper.readTree(view.get().yamlSource());
            } catch (IOException e) {
                log.warn("Profil-YAML fuer env {} nicht lesbar: {}", id, e.getMessage());
                return null;
            }
        });
    }

    private RuleEvaluationContext kontext(Finding f, JsonNode profil) {
        Cve cve = f.getCve();
        Component comp = f.getComponentOccurrence() == null
                ? null : f.getComponentOccurrence().getComponent();
        return new RuleEvaluationContext(
                cve == null ? null : new RuleEvaluationContext.CveSnapshot(
                        cve.getId(),
                        cve.getCveId(),
                        cve.getSummary(),
                        cve.getCwes(),
                        Boolean.TRUE.equals(cve.getKevListed()),
                        cve.getEpssScore(),
                        cve.getCvssBaseScore()),
                profil,
                comp == null ? null : new RuleEvaluationContext.ComponentSnapshot(
                        comp.getType(), comp.getName(), comp.getVersion()),
                new RuleEvaluationContext.FindingSnapshot(f.getId(), f.getDetectedAt()));
    }

    private List<Map<String, Object>> konfliktJson(List<DryRunResult.Conflict> conflicts) {
        List<Map<String, Object>> out = new ArrayList<>(conflicts.size());
        for (DryRunResult.Conflict c : conflicts) {
            Map<String, Object> m = new HashMap<>();
            m.put("findingId", c.findingId().toString());
            m.put("assessmentId", c.assessmentId().toString());
            m.put("approvedSeverity", c.approvedSeverity().name());
            m.put("ruleSeverity", c.ruleSeverity().name());
            out.add(m);
        }
        return out;
    }
}
